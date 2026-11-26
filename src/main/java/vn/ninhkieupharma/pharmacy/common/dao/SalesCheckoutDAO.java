package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.AllergyMatcher;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseInstructions;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseSession;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseTiming;
import vn.ninhkieupharma.pharmacy.domain.sales.DoseUnit;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineKind;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePricing;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleQuote;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockPreview;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockShortage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SalesCheckoutDAO {
    private static final long ROW_ID_PREFIX = 0x4e4b504841524d41L;
    private static final long MAX_UNSIGNED_INT = SaleOrderRequest.MAX_POINT_BALANCE;
    private static final BigDecimal MAX_SALE_AMOUNT = new BigDecimal("9999999999.99");
    private static final BigDecimal MAX_SALE_QUANTITY = new BigDecimal("999999999.999");
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ConnectionFactory connections;

    public SalesCheckoutDAO() {
        this(DatabaseConnection::getConnection);
    }

    SalesCheckoutDAO(ConnectionFactory connections) {
        this.connections = connections;
    }

    @FunctionalInterface
    interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    private record ProductState(
            long id, String name, String unit, boolean prescriptionRequired, BigDecimal price) {
    }

    private record Batch(long id, String code, String locationCode, String locationName,
                         LocalDate expiry, BigDecimal quantity) {
    }

    private record CustomerState(long id, String name, long points) {
    }

    private record Promotion(
            long id, String code, BigDecimal discountPercent,
            LocalDateTime redemptionStart, LocalDateTime earningEnd, BigDecimal redemptionRate) {
    }

    private record LoyaltyPolicy(BigDecimal pointsPerThousand, BigDecimal currencyPerPoint) {
    }

    private record PreparedSale(
            CustomerState customer, Map<Long, ProductState> products, List<BigDecimal> lineTotals,
            LocalDateTime soldAt, Promotion promotion, SaleQuote quote) {
    }

    @FunctionalInterface
    private interface SaleTransaction<T> {
        T execute(Connection connection) throws SQLException;
    }

    public List<SaleProduct> findSaleProducts() {
        String sql = """
                SELECT p.product_id, p.product_code, p.product_name, p.unit_name, p.barcode,
                       p.requires_prescription, p.selling_price,
                       COALESCE(SUM(CASE WHEN wl.location_id IS NOT NULL THEN b.quantity_on_hand ELSE 0 END), 0)
                           AS stock_on_hand,
                       COALESCE((SELECT GROUP_CONCAT(pi.ingredient_name
                           ORDER BY pi.ingredient_name SEPARATOR '\\n')
                           FROM product_ingredients pi WHERE pi.product_id = p.product_id), '') AS ingredients
                FROM products p
                LEFT JOIN inventory_batches b ON b.product_id = p.product_id AND b.expiry_date >= ?
                LEFT JOIN warehouse_locations wl ON wl.location_id = b.location_id AND wl.is_active = 1
                WHERE p.is_active = 1
                GROUP BY p.product_id, p.product_code, p.product_name, p.unit_name, p.barcode,
                         p.requires_prescription, p.selling_price
                ORDER BY p.product_name, p.product_id
                """;
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, LocalDate.now(APP_ZONE));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SaleProduct> products = new ArrayList<>();
                while (resultSet.next()) {
                    String encodedIngredients = resultSet.getString("ingredients");
                    List<String> ingredients = encodedIngredients == null || encodedIngredients.isBlank()
                            ? List.of()
                            : encodedIngredients.lines().toList();
                    products.add(new SaleProduct(
                            rowId(resultSet.getLong("product_id")),
                            resultSet.getString("product_code"),
                            resultSet.getString("product_name"),
                            resultSet.getString("unit_name"),
                            resultSet.getBoolean("requires_prescription"),
                            resultSet.getBigDecimal("selling_price"),
                            resultSet.getBigDecimal("stock_on_hand"),
                            ingredients,
                            resultSet.getString("barcode")));
                }
                return List.copyOf(products);
            }
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể tải danh sách sản phẩm bán."), exception);
        }
    }

    public List<SaleAllergyWarning> findAllergyWarnings(
            Optional<UUID> customerRowId, List<UUID> productRowIds) {
        if (customerRowId == null || customerRowId.isEmpty() || productRowIds == null || productRowIds.isEmpty()) {
            return List.of();
        }
        try (Connection connection = connections.open()) {
            return findAllergyWarnings(
                    connection, numericId(customerRowId.orElseThrow()),
                    productRowIds.stream().distinct().map(SalesCheckoutDAO::numericId).toList());
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể kiểm tra dị ứng của khách hàng."), exception);
        }
    }

    public List<SaleStockShortage> previewSaleStock(List<SaleLineRequest> lines) {
        return previewSalePicking(lines).shortages();
    }

    public SaleStockPreview previewSalePicking(List<SaleLineRequest> lines) {
        validateLines(lines);
        return inTransaction("Không thể kiểm tra tồn theo lô.", connection -> {
            Map<Long, ProductState> products = loadProducts(connection, lines, false);
            return planInventory(connection, lines, products, LocalDate.now(APP_ZONE), false);
        });
    }

    public SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        validateRequest(authenticatedUserId, request);
        return inTransaction("Không thể ghi giao dịch bán hàng.", connection -> {
            long employeeId = requireEmployee(connection, authenticatedUserId);
            PreparedSale sale = prepareSale(connection, newSaleCode(LocalDateTime.now(APP_ZONE)), request);
            requireSameWarnings(sale.quote().warnings(), request.acknowledgedAllergyWarnings());
            return recordSale(connection, authenticatedUserId, employeeId, null, request, sale);
        });
    }

    public SalePendingOrder createPendingSale(long authenticatedUserId, SaleOrderRequest request) {
        validateRequest(authenticatedUserId, request);
        return inTransaction("Không thể lưu đơn bán hàng đang chờ.", connection ->
                insertPendingSale(connection, requireEmployee(connection, authenticatedUserId), request));
    }

    public SalePendingOrder updatePendingSale(
            long authenticatedUserId, UUID saleRowId, long revision, SaleOrderRequest request) {
        validateRequest(authenticatedUserId, request);
        long saleId = numericId(saleRowId);
        return inTransaction("Không thể cập nhật đơn bán hàng đang chờ.", connection -> {
            long employeeId = requireEmployee(connection, authenticatedUserId);
            String saleCode = lockPendingSale(connection, saleId, revision);
            return replacePendingSale(connection, employeeId, saleId, saleCode, revision, request);
        });
    }

    public SalePendingOrder findPendingSale(UUID saleRowId) {
        long saleId = numericId(saleRowId);
        return inTransaction("Không thể tải đơn bán hàng đang chờ.", connection ->
                loadPendingSale(connection, saleId));
    }

    public SaleQuote quotePendingSale(
            long authenticatedUserId, UUID saleRowId, long revision, SaleOrderRequest request) {
        validateRequest(authenticatedUserId, request);
        long saleId = numericId(saleRowId);
        return inTransaction("Không thể tính số tiền cần thu.", connection -> {
            requireEmployee(connection, authenticatedUserId);
            String saleCode = lockPendingSale(connection, saleId, revision);
            return prepareSale(connection, saleCode, request).quote();
        });
    }

    public SaleReceipt completePendingSale(
            long authenticatedUserId, UUID saleRowId, long revision,
            SaleOrderRequest request, SaleQuote acceptedQuote) {
        validateRequest(authenticatedUserId, request);
        if (acceptedQuote == null) {
            throw new IllegalArgumentException(Localization.text("Cần xác nhận số tiền đã thu trước khi hoàn tất."));
        }
        long saleId = numericId(saleRowId);
        return inTransaction("Không thể hoàn tất đơn bán hàng.", connection -> {
            long employeeId = requireEmployee(connection, authenticatedUserId);
            String saleCode = lockPendingSale(connection, saleId, revision);
            PreparedSale sale = prepareSale(connection, saleCode, request);
            requireSameWarnings(sale.quote().warnings(), acceptedQuote.warnings());
            if (!sale.quote().amounts().equals(acceptedQuote.amounts())) {
                throw new IllegalStateException(Localization.text(
                        "Số tiền hoặc điểm của đơn đã thay đổi. Kiểm tra lại số cần thu trước khi hoàn tất."));
            }
            if (!sale.quote().picks().equals(acceptedQuote.picks())) {
                throw new IllegalStateException(Localization.text(
                        "Lô hoặc vị trí lấy thuốc đã thay đổi. Hãy kiểm tra lại danh sách lấy thuốc trước khi hoàn tất."));
            }
            return recordSale(connection, authenticatedUserId, employeeId, saleId, request, sale);
        });
    }

    public void cancelPendingSale(long authenticatedUserId, UUID saleRowId, long revision) {
        if (authenticatedUserId < 1) {
            throw new IllegalArgumentException(Localization.text(
                    "Cần có tài khoản nhân viên đăng nhập để hủy đơn."));
        }
        long saleId = numericId(saleRowId);
        inTransaction("Không thể hủy đơn bán hàng đang chờ.", connection -> {
            requireEmployee(connection, authenticatedUserId);
            lockPendingSale(connection, saleId, revision);
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE sales SET status = 'VOIDED' WHERE sale_id = ?
                    """)) {
                statement.setLong(1, saleId);
                statement.executeUpdate();
            }
            return null;
        });
    }

    public List<ModuleRow> findSaleDetails(UUID saleRowId) {
        long saleId = numericId(saleRowId);
        try (Connection connection = connections.open();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT si.sale_item_id, p.product_code, p.product_name, p.unit_name,
                            CAST(si.quantity AS CHAR), CAST(si.unit_price AS CHAR),
                            CAST(si.line_total AS CHAR), COALESCE(si.dosage_instructions, ''),
                            CAST(si.treatment_days AS CHAR), si.line_kind, si.dose_amount,
                            si.dose_unit, si.dose_timing, si.dose_sessions
                     FROM sale_items si
                     JOIN products p ON p.product_id = si.product_id
                     WHERE si.sale_id = ?
                     ORDER BY si.sale_item_id
                     """)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ModuleRow> details = new ArrayList<>();
                while (resultSet.next()) {
                    SaleLineKind kind = readEnum(resultSet, "line_kind", SaleLineKind.class);
                    details.add(new ModuleRow(rowId(resultSet.getLong("sale_item_id")), List.of(
                            resultSet.getString("product_code"),
                            resultSet.getString("product_name"),
                            resultSet.getString("unit_name"),
                            resultSet.getString(5),
                            resultSet.getString(6),
                            resultSet.getString(7),
                            formatDosage(readDose(resultSet), resultSet.getString(8)),
                            resultSet.getString(9),
                            kind == null ? "—" : Localization.text(kind.label()))));
                }
                return List.copyOf(details);
            }
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể tải chi tiết đơn bán hàng."), exception);
        }
    }

    private PreparedSale prepareSale(Connection connection, String saleCode, SaleOrderRequest request)
            throws SQLException {
        CustomerState customer = requireCustomer(connection, numericId(request.customerId().orElseThrow()));
        Map<Long, ProductState> products = loadProducts(connection, request.lines(), true);
        validatePrescription(request, products);

        List<SaleAllergyWarning> warnings = findAllergyWarnings(
                connection, customer.id(),
                request.lines().stream().map(line -> numericId(line.productId())).distinct().toList());
        List<BigDecimal> lineTotals = lineTotals(request.lines(), products);
        BigDecimal subtotal = subtotal(lineTotals);
        LocalDateTime soldAt = LocalDateTime.now(APP_ZONE);
        List<SaleStockPreview.Pick> picks = requireInventory(
                connection, request.lines(), products, soldAt.toLocalDate());
        Promotion promotion = findBestPromotion(connection, soldAt);
        LoyaltyPolicy loyalty = findLoyaltyPolicy(connection);
        BigDecimal redemptionRate = redemptionRate(promotion, loyalty, soldAt);
        long pointsBalance = customer.points();
        BigDecimal promotionPercent = promotion == null ? BigDecimal.ZERO : promotion.discountPercent();
        SalePricing.Result pricing = SalePricing.calculate(
                subtotal, promotionPercent, pointsBalance, request.requestedPoints(),
                redemptionRate, BigDecimal.ZERO, true);
        BigDecimal pointsRatePercent = findPointsRate(
                connection, promotion, soldAt, pricing.total(), loyalty);
        pricing = SalePricing.calculate(
                subtotal, promotionPercent, pointsBalance, request.requestedPoints(),
                redemptionRate, pointsRatePercent, true);
        long pointsRedeemed = pricing.pointsRedeemed();
        long pointsEarned = pricing.pointsEarned();
        long remainingPoints = pointsBalance - pointsRedeemed + pointsEarned;
        if (remainingPoints < 0 || remainingPoints > MAX_UNSIGNED_INT
                || pointsEarned > MAX_UNSIGNED_INT || pointsRedeemed > MAX_UNSIGNED_INT) {
            throw new IllegalStateException(Localization.text(
                    "Điểm tích lũy vượt quá giới hạn dữ liệu được hỗ trợ."));
        }

        SaleReceipt amounts = new SaleReceipt(
                saleCode, subtotal, pricing.promotionDiscount(), pricing.pointsDiscount(), pricing.total(),
                pointsRedeemed, pointsEarned, remainingPoints, promotion == null ? "" : promotion.code());
        return new PreparedSale(customer, products, lineTotals, soldAt, promotion,
                new SaleQuote(amounts, warnings, picks));
    }

    private static SaleReceipt recordSale(
            Connection connection, long authenticatedUserId, long employeeId, Long pendingSaleId,
            SaleOrderRequest request, PreparedSale sale) throws SQLException {
        SaleReceipt receipt = sale.quote().amounts();
        long saleId;
        if (pendingSaleId == null) {
            saleId = insertSale(connection, sale.customer().id(), employeeId, sale, request);
        } else {
            saleId = pendingSaleId;
            updateCompletedSale(connection, saleId, sale.customer().id(), employeeId, sale, request);
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM sale_items WHERE sale_id = ?")) {
                statement.setLong(1, saleId);
                statement.executeUpdate();
            }
        }
        insertSaleLines(connection, saleId, request.lines(), sale.products(), sale.lineTotals());
        recordInventory(connection, saleId, authenticatedUserId, sale);
        updateCustomerPoints(connection, sale.customer().id(), sale.customer().points(),
                receipt.pointsRedeemed(), receipt.pointsEarned());
        return receipt;
    }

    private SalePendingOrder insertPendingSale(
            Connection connection, long employeeId, SaleOrderRequest request) throws SQLException {
        CustomerState customer = requireCustomer(connection, numericId(request.customerId().orElseThrow()));
        Map<Long, ProductState> products = loadProducts(connection, request.lines(), true);
        validatePrescription(request, products);
        LocalDateTime createdAt = LocalDateTime.now(APP_ZONE);
        requireInventory(connection, request.lines(), products, createdAt.toLocalDate());
        List<BigDecimal> lineTotals = lineTotals(request.lines(), products);
        BigDecimal subtotal = subtotal(lineTotals);
        validatePendingRedemption(connection, subtotal, customer.points(), request.requestedPoints(), createdAt);

        String saleCode = newSaleCode(createdAt);
        long saleId;
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO sales
                    (sale_code, customer_id, employee_id, sold_at, subtotal,
                     promotion_discount_amount, points_discount_amount, discount_amount, total_amount,
                     points_earned, points_redeemed, status, redeem_points, requested_points, prescription_code,
                     prescription_date, prescribing_doctor, clinic, diagnosis, note)
                VALUES (?, ?, ?, ?, ?, 0, 0, 0, ?, 0, 0, 'PENDING', ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, saleCode);
            statement.setLong(2, customer.id());
            statement.setLong(3, employeeId);
            statement.setObject(4, createdAt);
            statement.setBigDecimal(5, subtotal);
            statement.setBigDecimal(6, subtotal);
            bindPointsAndPrescription(statement, 7, request);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL did not return the generated pending sale id.");
                }
                saleId = keys.getLong(1);
            }
        }
        insertSaleLines(connection, saleId, request.lines(), products, lineTotals);
        return new SalePendingOrder(rowId(saleId), saleCode, request, subtotal, 0);
    }

    private SalePendingOrder replacePendingSale(
            Connection connection, long employeeId, long saleId, String saleCode,
            long revision, SaleOrderRequest request)
            throws SQLException {
        CustomerState customer = requireCustomer(connection, numericId(request.customerId().orElseThrow()));
        Map<Long, ProductState> products = loadProducts(connection, request.lines(), true);
        validatePrescription(request, products);
        LocalDateTime updatedAt = LocalDateTime.now(APP_ZONE);
        requireInventory(connection, request.lines(), products, updatedAt.toLocalDate());
        List<BigDecimal> lineTotals = lineTotals(request.lines(), products);
        BigDecimal subtotal = subtotal(lineTotals);
        validatePendingRedemption(connection, subtotal, customer.points(), request.requestedPoints(), updatedAt);

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sales
                SET customer_id = ?, employee_id = ?, sold_at = ?, subtotal = ?,
                    promotion_id = NULL, promotion_discount_amount = 0, points_discount_amount = 0,
                    discount_amount = 0, total_amount = ?, points_earned = 0, points_redeemed = 0,
                    redeem_points = ?, requested_points = ?, revision = revision + 1,
                    prescription_code = ?, prescription_date = ?,
                    prescribing_doctor = ?, clinic = ?, diagnosis = ?, note = ?
                WHERE sale_id = ? AND status = 'PENDING'
                """)) {
            statement.setLong(1, customer.id());
            statement.setLong(2, employeeId);
            statement.setObject(3, updatedAt);
            statement.setBigDecimal(4, subtotal);
            statement.setBigDecimal(5, subtotal);
            bindPointsAndPrescription(statement, 6, request);
            statement.setLong(14, saleId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(Localization.text("Chỉ đơn đang chờ mới có thể được chỉnh sửa."));
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM sale_items WHERE sale_id = ?")) {
            statement.setLong(1, saleId);
            statement.executeUpdate();
        }
        insertSaleLines(connection, saleId, request.lines(), products, lineTotals);
        return new SalePendingOrder(rowId(saleId), saleCode, request, subtotal, revision + 1);
    }

    private static String lockPendingSale(Connection connection, long saleId, long revision) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sale_code FROM sales
                WHERE sale_id = ? AND status = 'PENDING' AND revision = ?
                FOR UPDATE
                """)) {
            statement.setLong(1, saleId);
            statement.setLong(2, revision);
            try (ResultSet rows = statement.executeQuery()) {
                if (rows.next()) {
                    return rows.getString(1);
                }
            }
        }
        throw new IllegalStateException(Localization.text(
                "Đơn đang chờ đã thay đổi hoặc đã đóng. Hãy làm mới danh sách và mở lại đơn."));
    }

    private SalePendingOrder loadPendingSale(Connection connection, long saleId)
            throws SQLException {
        String sql = """
                SELECT sale_code, customer_id, subtotal, redeem_points, requested_points, revision, prescription_code,
                       prescription_date, prescribing_doctor, clinic, diagnosis, note
                FROM sales
                WHERE sale_id = ? AND status = 'PENDING'
                FOR UPDATE
                """;
        String saleCode;
        Optional<UUID> customerId;
        BigDecimal subtotal;
        long requestedPoints;
        long revision;
        String prescriptionCode;
        LocalDate prescriptionDate;
        String doctor;
        String clinic;
        String diagnosis;
        String note;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(Localization.text("Không tìm thấy đơn đang chờ."));
                }
                saleCode = resultSet.getString("sale_code");
                long storedCustomerId = resultSet.getLong("customer_id");
                customerId = resultSet.wasNull() ? Optional.empty() : Optional.of(rowId(storedCustomerId));
                subtotal = resultSet.getBigDecimal("subtotal");
                requestedPoints = resultSet.getLong("requested_points");
                if (resultSet.wasNull() && resultSet.getBoolean("redeem_points")) {
                    requestedPoints = SaleOrderRequest.REDEEM_MAXIMUM;
                }
                revision = resultSet.getLong("revision");
                prescriptionCode = resultSet.getString("prescription_code");
                java.sql.Date date = resultSet.getDate("prescription_date");
                prescriptionDate = date == null ? null : date.toLocalDate();
                doctor = resultSet.getString("prescribing_doctor");
                clinic = resultSet.getString("clinic");
                diagnosis = resultSet.getString("diagnosis");
                note = resultSet.getString("note");
            }
        }

        List<SaleLineRequest> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT product_id, quantity, dosage_instructions, treatment_days,
                       line_kind, dose_amount, dose_unit, dose_timing, dose_sessions
                FROM sale_items
                WHERE sale_id = ?
                ORDER BY sale_item_id
                """)) {
            statement.setLong(1, saleId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    lines.add(new SaleLineRequest(
                            rowId(resultSet.getLong("product_id")),
                            resultSet.getBigDecimal("quantity"),
                            resultSet.getString("dosage_instructions"),
                            resultSet.getInt("treatment_days"),
                            readEnum(resultSet, "line_kind", SaleLineKind.class),
                            readDose(resultSet)));
                }
            }
        }
        SaleOrderRequest request = new SaleOrderRequest(
                customerId, lines, prescriptionCode, prescriptionDate,
                doctor, clinic, diagnosis, note, requestedPoints, List.of());
        return new SalePendingOrder(rowId(saleId), saleCode, request, subtotal, revision);
    }

    private static <T extends Enum<T>> T readEnum(ResultSet resultSet, String column, Class<T> type)
            throws SQLException {
        String value = resultSet.getString(column);
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static DoseInstructions readDose(ResultSet resultSet) throws SQLException {
        int sessionMask = resultSet.getInt("dose_sessions");
        EnumSet<DoseSession> sessions = EnumSet.noneOf(DoseSession.class);
        for (DoseSession session : DoseSession.values()) {
            if ((sessionMask & session.mask()) != 0) {
                sessions.add(session);
            }
        }
        return new DoseInstructions(
                resultSet.getBigDecimal("dose_amount"),
                readEnum(resultSet, "dose_unit", DoseUnit.class),
                readEnum(resultSet, "dose_timing", DoseTiming.class),
                sessions);
    }

    private static String formatDosage(DoseInstructions dose, String notes) {
        List<String> instructions = new ArrayList<>();
        if (dose.amount() != null && dose.unit() != null) {
            NumberFormat formatter = NumberFormat.getNumberInstance(
                    Localization.isEnglish() ? Locale.US : Locale.forLanguageTag("vi-VN"));
            formatter.setMaximumFractionDigits(3);
            instructions.add(formatter.format(dose.amount()) + " " + Localization.text(dose.unit().label()));
        }
        if (dose.timing() != null) {
            instructions.add(Localization.text(dose.timing().label()));
        }
        if (!dose.sessions().isEmpty()) {
            List<String> sessions = EnumSet.copyOf(dose.sessions()).stream()
                    .map(session -> Localization.text(session.label()))
                    .toList();
            instructions.add(String.join(", ", sessions));
        }
        if (!blank(notes)) {
            instructions.add(notes);
        }
        return String.join(" · ", instructions);
    }

    private static List<BigDecimal> lineTotals(
            List<SaleLineRequest> lines, Map<Long, ProductState> products) {
        List<BigDecimal> totals = new ArrayList<>(lines.size());
        for (SaleLineRequest line : lines) {
            BigDecimal amount = line.quantity()
                    .multiply(products.get(numericId(line.productId())).price())
                    .setScale(2, RoundingMode.HALF_UP);
            requireSaleAmount(amount);
            totals.add(amount);
        }
        return List.copyOf(totals);
    }

    private static BigDecimal subtotal(List<BigDecimal> lineTotals) {
        BigDecimal subtotal = lineTotals.stream().reduce(ZERO_MONEY, BigDecimal::add);
        requireSaleAmount(subtotal);
        return subtotal;
    }

    private static String newSaleCode(LocalDateTime createdAt) {
        return "HD" + createdAt.toLocalDate().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static void insertSaleLines(
            Connection connection, long saleId, List<SaleLineRequest> lines,
            Map<Long, ProductState> products, List<BigDecimal> lineTotals) throws SQLException {
        for (int index = 0; index < lines.size(); index++) {
            SaleLineRequest line = lines.get(index);
            ProductState product = products.get(numericId(line.productId()));
            insertSaleLine(connection, saleId, product.id(), line, product.price(), lineTotals.get(index));
        }
    }

    private static void bindPointsAndPrescription(
            PreparedStatement statement, int firstIndex, SaleOrderRequest request) throws SQLException {
        statement.setBoolean(firstIndex, request.redeemPoints());
        setNullableLong(statement, firstIndex + 1, request.requestedPoints() == SaleOrderRequest.REDEEM_MAXIMUM
                ? null : request.requestedPoints());
        firstIndex += 2;
        setNullableString(statement, firstIndex, request.prescriptionCode());
        if (request.prescriptionDate() == null) {
            statement.setNull(firstIndex + 1, Types.DATE);
        } else {
            statement.setDate(firstIndex + 1, java.sql.Date.valueOf(request.prescriptionDate()));
        }
        setNullableString(statement, firstIndex + 2, request.prescribingDoctor());
        setNullableString(statement, firstIndex + 3, request.clinic());
        setNullableString(statement, firstIndex + 4, request.diagnosis());
        setNullableString(statement, firstIndex + 5, request.note());
    }

    private <T> T inTransaction(String failureMessage, SaleTransaction<T> transaction) {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                T result = transaction.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw DataAccessException.fromSql(exception, failureMessage);
        }
    }

    private static long requireEmployee(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT e.employee_id
                FROM employees e
                JOIN roles r ON r.role_id = e.role_id
                WHERE e.employee_id = ? AND e.is_active = 1 AND r.role_code = 'STAFF'
                  AND e.employee_code IS NOT NULL AND e.employment_status = 'ACTIVE'
                """)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(Localization.text(
                            "Chỉ nhân viên đang hoạt động mới được xử lý đơn bán hàng."));
                }
                return resultSet.getLong(1);
            }
        }
    }

    private static CustomerState requireCustomer(Connection connection, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT customer_id, full_name, loyalty_points
                FROM customers
                WHERE customer_id = ? AND is_active = 1 AND is_blocked = 0
                FOR UPDATE
                """)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(Localization.text(
                            "Khách hàng đã chọn không còn đủ điều kiện bán."));
                }
                return new CustomerState(
                        resultSet.getLong("customer_id"),
                        resultSet.getString("full_name"),
                        resultSet.getLong("loyalty_points"));
            }
        }
    }

    private static Map<Long, ProductState> loadProducts(
            Connection connection, List<SaleLineRequest> lines, boolean lock) throws SQLException {
        List<Long> productIds = lines.stream().map(line -> numericId(line.productId()))
                .distinct().sorted().toList();
        Map<Long, ProductState> products = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT product_id, product_name, unit_name, requires_prescription, selling_price
                FROM products
                WHERE product_id = ? AND is_active = 1
                """ + (lock ? "FOR UPDATE" : ""))) {
            for (long productId : productIds) {
                statement.setLong(1, productId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException(Localization.text(
                                "Một sản phẩm đã chọn không còn hoạt động."));
                    }
                    BigDecimal price = resultSet.getBigDecimal("selling_price");
                    if (price == null || price.signum() <= 0) {
                        throw new IllegalStateException(Localization.text(
                                "Cần đặt giá bán lớn hơn 0 trước khi bán sản phẩm này."));
                    }
                    products.put(productId, new ProductState(
                            productId, resultSet.getString("product_name"),
                            resultSet.getString("unit_name"),
                            resultSet.getBoolean("requires_prescription"), price));
                }
            }
        }
        return products;
    }

    private static void validatePrescription(SaleOrderRequest request, Map<Long, ProductState> products) {
        boolean needsPrescription = false;
        for (SaleLineRequest line : request.lines()) {
            ProductState product = products.get(numericId(line.productId()));
            if (product.prescriptionRequired() && line.kind() == SaleLineKind.ADD_ON) {
                throw new IllegalArgumentException(Localization.text(
                        "Thuốc bắt buộc kê đơn phải được đánh dấu là thuốc kê đơn."));
            }
            needsPrescription |= product.prescriptionRequired() || line.kind() == SaleLineKind.PRESCRIBED;
        }
        if (needsPrescription && (blank(request.prescriptionCode()) || request.prescriptionDate() == null
                || blank(request.prescribingDoctor()))) {
            throw new IllegalArgumentException(Localization.text(
                    "Thuốc kê đơn yêu cầu mã toa, ngày kê hợp lệ và tên bác sĩ kê đơn."));
        }
    }

    private static Promotion findBestPromotion(Connection connection, LocalDateTime now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT promotion_id, promotion_code, discount_percent,
                       points_redemption_starts_at, points_earning_ends_at, redemption_vnd_per_point
                FROM promotions
                WHERE is_active = 1 AND starts_at <= ? AND ends_at >= ?
                ORDER BY discount_percent DESC, promotion_id
                LIMIT 1
                """)) {
            statement.setObject(1, now);
            statement.setObject(2, now);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new Promotion(
                        resultSet.getLong("promotion_id"),
                        resultSet.getString("promotion_code"),
                        resultSet.getBigDecimal("discount_percent"),
                        resultSet.getTimestamp("points_redemption_starts_at").toLocalDateTime(),
                        resultSet.getTimestamp("points_earning_ends_at").toLocalDateTime(),
                        resultSet.getBigDecimal("redemption_vnd_per_point"));
            }
        }
    }

    private static LoyaltyPolicy findLoyaltyPolicy(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT points_per_1000_currency, currency_per_point
                FROM loyalty_programs
                WHERE is_active = 1
                ORDER BY loyalty_program_id
                LIMIT 1
                """);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next()
                    ? new LoyaltyPolicy(resultSet.getBigDecimal(1), resultSet.getBigDecimal(2))
                    : null;
        }
    }

    private static BigDecimal redemptionRate(Promotion promotion, LoyaltyPolicy loyalty, LocalDateTime at) {
        if (promotion != null && !at.isBefore(promotion.redemptionStart())) {
            return promotion.redemptionRate();
        }
        return loyalty == null ? BigDecimal.ZERO : loyalty.currencyPerPoint();
    }

    private static void validatePendingRedemption(
            Connection connection, BigDecimal subtotal, long availablePoints, long requestedPoints,
            LocalDateTime at) throws SQLException {
        if (requestedPoints == 0 || requestedPoints == SaleOrderRequest.REDEEM_MAXIMUM) {
            return;
        }
        Promotion promotion = findBestPromotion(connection, at);
        LoyaltyPolicy loyalty = findLoyaltyPolicy(connection);
        BigDecimal promotionPercent = promotion == null ? BigDecimal.ZERO : promotion.discountPercent();
        SalePricing.calculate(subtotal, promotionPercent, availablePoints, requestedPoints,
                redemptionRate(promotion, loyalty, at), BigDecimal.ZERO, true);
    }

    private static BigDecimal findPointsRate(
            Connection connection,
            Promotion promotion,
            LocalDateTime now,
            BigDecimal finalTotal,
            LoyaltyPolicy loyalty) throws SQLException {
        if (promotion != null && !now.isAfter(promotion.earningEnd())) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT points_rate_percent
                    FROM promotion_point_tiers
                    WHERE promotion_id = ? AND minimum_invoice_total_vnd <= ?
                    ORDER BY minimum_invoice_total_vnd DESC
                    LIMIT 1
                    """)) {
                statement.setLong(1, promotion.id());
                statement.setBigDecimal(2, finalTotal);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getBigDecimal(1);
                    }
                }
            }
        }
        return loyalty == null
                ? BigDecimal.ZERO
                : loyalty.pointsPerThousand().divide(new BigDecimal("10"), 8, RoundingMode.HALF_UP);
    }

    private static long insertSale(
            Connection connection, long customerId, long employeeId,
            PreparedSale sale, SaleOrderRequest request)
            throws SQLException {
        SaleReceipt receipt = sale.quote().amounts();
        String sql = """
                INSERT INTO sales
                    (sale_code, customer_id, employee_id, promotion_id, sold_at, subtotal,
                     promotion_discount_amount, points_discount_amount, discount_amount, total_amount,
                     points_earned, points_redeemed, status, redeem_points, requested_points,
                     prescription_code, prescription_date, prescribing_doctor, clinic, diagnosis, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, receipt.saleCode());
            statement.setLong(2, customerId);
            statement.setLong(3, employeeId);
            bindCompletedSale(statement, 4, sale, request);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL did not return the generated sale id.");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void updateCompletedSale(
            Connection connection, long saleId, long customerId, long employeeId,
            PreparedSale sale, SaleOrderRequest request)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sales
                SET customer_id = ?, employee_id = ?, promotion_id = ?, sold_at = ?, subtotal = ?,
                    promotion_discount_amount = ?, points_discount_amount = ?, discount_amount = ?,
                    total_amount = ?, points_earned = ?, points_redeemed = ?, status = 'COMPLETED',
                    redeem_points = ?, requested_points = ?, prescription_code = ?, prescription_date = ?,
                    prescribing_doctor = ?, clinic = ?, diagnosis = ?, note = ?
                WHERE sale_id = ? AND status = 'PENDING'
                """)) {
            statement.setLong(1, customerId);
            statement.setLong(2, employeeId);
            bindCompletedSale(statement, 3, sale, request);
            statement.setLong(20, saleId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException(Localization.text("Chỉ đơn đang chờ mới có thể hoàn tất."));
            }
        }
    }

    private static void bindCompletedSale(
            PreparedStatement statement, int firstIndex, PreparedSale sale, SaleOrderRequest request)
            throws SQLException {
        SaleReceipt receipt = sale.quote().amounts();
        setNullableLong(statement, firstIndex, sale.promotion() == null ? null : sale.promotion().id());
        statement.setObject(firstIndex + 1, sale.soldAt());
        statement.setBigDecimal(firstIndex + 2, receipt.subtotal());
        statement.setBigDecimal(firstIndex + 3, receipt.promotionDiscount());
        statement.setBigDecimal(firstIndex + 4, receipt.pointsDiscount());
        statement.setBigDecimal(firstIndex + 5, receipt.promotionDiscount().add(receipt.pointsDiscount()));
        statement.setBigDecimal(firstIndex + 6, receipt.total());
        statement.setLong(firstIndex + 7, receipt.pointsEarned());
        statement.setLong(firstIndex + 8, receipt.pointsRedeemed());
        bindPointsAndPrescription(statement, firstIndex + 9, request);
    }

    private static void insertSaleLine(
            Connection connection, long saleId, long productId, SaleLineRequest line,
            BigDecimal price, BigDecimal total) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO sale_items
                    (sale_id, product_id, quantity, unit_price, line_discount, line_total,
                     dosage_instructions, treatment_days, line_kind, dose_amount, dose_unit,
                     dose_timing, dose_sessions)
                VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setLong(1, saleId);
            statement.setLong(2, productId);
            statement.setBigDecimal(3, line.quantity());
            statement.setBigDecimal(4, price);
            statement.setBigDecimal(5, total);
            setNullableString(statement, 6, line.dosageInstructions());
            statement.setInt(7, line.treatmentDays());
            setNullableString(statement, 8, line.kind() == null ? null : line.kind().name());
            DoseInstructions dose = line.dose();
            statement.setBigDecimal(9, dose.amount());
            setNullableString(statement, 10, dose.unit() == null ? null : dose.unit().name());
            setNullableString(statement, 11, dose.timing() == null ? null : dose.timing().name());
            int sessionMask = dose.sessions().stream().mapToInt(DoseSession::mask).reduce(0, (left, right) -> left | right);
            statement.setInt(12, sessionMask);
            statement.executeUpdate();
        }
    }

    private static List<SaleStockPreview.Pick> requireInventory(
            Connection connection, List<SaleLineRequest> lines,
            Map<Long, ProductState> products, LocalDate saleDate) throws SQLException {
        SaleStockPreview plan = planInventory(connection, lines, products, saleDate, true);
        if (!plan.shortages().isEmpty()) {
            SaleStockShortage shortage = plan.shortages().getFirst();
            throw new IllegalStateException(Localization.text("Không đủ tồn kho chưa hết hạn cho ")
                    + shortage.productName()
                    + Localization.text(" đến hết thời gian điều trị đã khai báo."));
        }
        return plan.picks();
    }

    private static SaleStockPreview planInventory(
            Connection connection, List<SaleLineRequest> lines,
            Map<Long, ProductState> products, LocalDate saleDate, boolean lock) throws SQLException {
        Map<Long, List<Batch>> batchesByProduct = new HashMap<>();
        Map<Long, BigDecimal> remainingByBatch = new HashMap<>();
        for (long productId : products.keySet().stream().sorted().toList()) {
            List<Batch> batches = loadEligibleBatches(connection, productId, saleDate, lock);
            batchesByProduct.put(productId, batches);
            for (Batch batch : batches) {
                remainingByBatch.put(batch.id(), batch.quantity());
            }
        }
        List<SaleLineRequest> allocationOrder = lines.stream()
                .sorted(Comparator.comparingInt(SaleLineRequest::treatmentDays).reversed()
                        .thenComparing(line -> numericId(line.productId())))
                .toList();
        List<SaleStockPreview.Pick> picks = new ArrayList<>();
        List<SaleStockShortage> shortages = new ArrayList<>();
        for (SaleLineRequest line : allocationOrder) {
            long productId = numericId(line.productId());
            ProductState product = products.get(productId);
            LocalDate lastTreatmentDate = saleDate.plusDays(line.treatmentDays() - 1L);
            BigDecimal remaining = line.quantity();
            for (Batch batch : batchesByProduct.get(productId)) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal available = remainingByBatch.get(batch.id());
                if (batch.expiry().isBefore(lastTreatmentDate) || available.signum() <= 0) {
                    continue;
                }
                BigDecimal allocated = remaining.min(available);
                remainingByBatch.put(batch.id(), available.subtract(allocated));
                picks.add(new SaleStockPreview.Pick(batch.id(), product.name(), product.unit(), batch.code(),
                        batch.locationCode(), batch.locationName(), batch.expiry(), allocated.stripTrailingZeros()));
                remaining = remaining.subtract(allocated);
            }
            if (remaining.signum() > 0) {
                shortages.add(new SaleStockShortage(product.name(), lastTreatmentDate,
                        line.quantity(), line.quantity().subtract(remaining)));
            }
        }
        return new SaleStockPreview(picks, shortages);
    }

    private static void recordInventory(
            Connection connection, long saleId, long createdByUserId, PreparedSale sale) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE inventory_batches
                SET quantity_on_hand = quantity_on_hand - ?
                WHERE batch_id = ? AND quantity_on_hand >= ?
                """)) {
            for (SaleStockPreview.Pick pick : sale.quote().picks()) {
                update.setBigDecimal(1, pick.quantity());
                update.setLong(2, pick.batchId());
                update.setBigDecimal(3, pick.quantity());
                if (update.executeUpdate() != 1) {
                    throw new SQLException("Inventory changed while the sale was being completed.");
                }
                insertSaleMovement(connection, pick.batchId(), createdByUserId, saleId, pick.quantity(),
                        pick.productName(), sale.soldAt());
            }
        }
    }

    private static List<Batch> loadEligibleBatches(
            Connection connection, long productId, LocalDate saleDate, boolean lock) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT b.batch_id, b.batch_code, wl.location_code, wl.location_name, b.expiry_date, b.quantity_on_hand
                FROM inventory_batches b
                JOIN warehouse_locations wl ON wl.location_id = b.location_id AND wl.is_active = 1
                WHERE b.product_id = ? AND b.expiry_date >= ? AND b.quantity_on_hand > 0
                ORDER BY b.expiry_date, b.batch_id
                """ + (lock ? "FOR UPDATE" : ""))) {
            statement.setLong(1, productId);
            statement.setObject(2, saleDate);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Batch> batches = new ArrayList<>();
                while (resultSet.next()) {
                    batches.add(new Batch(
                            resultSet.getLong("batch_id"),
                            resultSet.getString("batch_code"),
                            resultSet.getString("location_code"),
                            resultSet.getString("location_name"),
                            resultSet.getDate("expiry_date").toLocalDate(),
                            resultSet.getBigDecimal("quantity_on_hand")));
                }
                return batches;
            }
        }
    }

    private static void insertSaleMovement(
            Connection connection, long batchId, long createdByUserId, long saleId,
            BigDecimal quantity, String productName, LocalDateTime soldAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO inventory_movements
                    (batch_id, movement_type, quantity_delta, source_type, source_id,
                     moved_at, created_by, note)
                VALUES (?, 'SALE', ?, 'SALE', ?, ?, ?, ?)
                """)) {
            statement.setLong(1, batchId);
            statement.setBigDecimal(2, quantity.negate());
            statement.setLong(3, saleId);
            statement.setObject(4, soldAt);
            statement.setLong(5, createdByUserId);
            statement.setString(6, "Sale " + saleId + " · " + productName);
            statement.executeUpdate();
        }
    }

    private static void updateCustomerPoints(
            Connection connection, long customerId, long balance, long redeemed, long earned)
            throws SQLException {
        if (balance - redeemed + earned > MAX_UNSIGNED_INT) {
            throw new IllegalStateException(Localization.text(
                    "Điểm tích lũy của khách hàng sẽ vượt quá giới hạn."));
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE customers
                SET loyalty_points = loyalty_points - ? + ?
                WHERE customer_id = ? AND loyalty_points = ? AND is_active = 1 AND is_blocked = 0
                """)) {
            statement.setLong(1, redeemed);
            statement.setLong(2, earned);
            statement.setLong(3, customerId);
            statement.setLong(4, balance);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Customer points changed while the sale was being completed.");
            }
        }
    }

    private static List<SaleAllergyWarning> findAllergyWarnings(
            Connection connection, long customerId, List<Long> productIds) throws SQLException {
        if (productIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(productIds.size(), "?"));
        String sql = """
                SELECT p.product_id, p.product_name, ca.allergen_name, pi.ingredient_name
                FROM customer_allergies ca
                JOIN product_ingredients pi ON pi.product_id IN (
                """ + placeholders + """
                )
                JOIN products p ON p.product_id = pi.product_id
                WHERE ca.customer_id = ?
                ORDER BY p.product_name, pi.ingredient_name
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < productIds.size(); index++) {
                statement.setLong(index + 1, productIds.get(index));
            }
            statement.setLong(productIds.size() + 1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SaleAllergyWarning> warnings = new ArrayList<>();
                while (resultSet.next()) {
                    String allergen = resultSet.getString("allergen_name");
                    String ingredient = resultSet.getString("ingredient_name");
                    if (AllergyMatcher.matches(allergen, ingredient)) {
                        warnings.add(new SaleAllergyWarning(
                                rowId(resultSet.getLong("product_id")),
                                resultSet.getString("product_name"), allergen, ingredient));
                    }
                }
                return List.copyOf(warnings);
            }
        }
    }

    private static java.util.Set<String> warningKeys(List<SaleAllergyWarning> warnings) {
        return warnings.stream()
                .map(warning -> warning.productId() + "|"
                        + AllergyMatcher.canonicalize(warning.allergenName()) + "|"
                        + AllergyMatcher.canonicalize(warning.ingredientName()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static void requireSameWarnings(List<SaleAllergyWarning> current, List<SaleAllergyWarning> accepted) {
        if (!warningKeys(current).equals(warningKeys(accepted))) {
            throw new IllegalStateException(Localization.text(
                    "Thông tin dị ứng của khách đã thay đổi. Hãy xem lại cảnh báo trước khi hoàn tất đơn."));
        }
    }

    private static void validateRequest(long authenticatedUserId, SaleOrderRequest request) {
        if (authenticatedUserId < 1) {
            throw new IllegalArgumentException(Localization.text(
                    "Cần có tài khoản nhân viên đăng nhập để hoàn tất đơn."));
        }
        if (request == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException(Localization.text(
                    "Vui lòng thêm ít nhất một sản phẩm vào giỏ."));
        }
        if (request.customerId().isEmpty()) {
            throw new IllegalArgumentException(Localization.text(
                    "Cần chọn hồ sơ khách hàng trước khi lưu hoặc hoàn tất đơn."));
        }
        numericId(request.customerId().orElseThrow());
        if (request.requestedPoints() < 0 || (request.requestedPoints() > MAX_UNSIGNED_INT
                && request.requestedPoints() != SaleOrderRequest.REDEEM_MAXIMUM)) {
            throw new IllegalArgumentException(Localization.text("Số điểm muốn đổi vượt quá phạm vi được hỗ trợ."));
        }
        validateLines(request.lines());
        LocalDate prescriptionDate = request.prescriptionDate();
        if (prescriptionDate != null && (prescriptionDate.getYear() < 1000
                || prescriptionDate.isAfter(LocalDate.now(APP_ZONE)))) {
            throw new IllegalArgumentException(Localization.text("Ngày kê đơn phải hợp lệ và không ở tương lai."));
        }
        requireMaxLength(request.prescriptionCode(), 80, "Mã toa thuốc");
        requireMaxLength(request.prescribingDoctor(), 120, "Tên bác sĩ");
        requireMaxLength(request.clinic(), 180, "Tên cơ sở khám");
        requireMaxLength(request.diagnosis(), 255, "Chẩn đoán");
        requireMaxLength(request.note(), 500, "Ghi chú đơn hàng");
    }

    private static void validateLines(List<SaleLineRequest> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException(Localization.text("Vui lòng thêm ít nhất một sản phẩm vào giỏ."));
        }
        for (SaleLineRequest line : lines) {
            if (line == null || line.productId() == null || line.quantity() == null
                    || line.quantity().signum() <= 0 || line.quantity().scale() > 3
                    || line.treatmentDays() < 1 || line.treatmentDays() > 3650) {
                throw new IllegalArgumentException(Localization.text(
                        "Mỗi dòng cần có sản phẩm, số lượng dương và thời gian điều trị từ 1 đến 3650 ngày."));
            }
            numericId(line.productId());
            if (line.quantity().compareTo(MAX_SALE_QUANTITY) > 0) {
                throw new IllegalArgumentException(Localization.text(
                        "Số lượng bán vượt quá phạm vi được hỗ trợ."));
            }
            requireMaxLength(line.dosageInstructions(), 255, "Hướng dẫn sử dụng");
            if (!line.dose().hasValidAmount()) {
                throw new IllegalArgumentException(Localization.text(DoseInstructions.AMOUNT_ERROR));
            }
        }
    }

    private static void requireSaleAmount(BigDecimal amount) {
        if (amount.compareTo(MAX_SALE_AMOUNT) > 0) {
            throw new IllegalArgumentException(Localization.text(
                    "Giá trị đơn hàng vượt quá phạm vi dữ liệu được hỗ trợ."));
        }
    }

    private static void requireMaxLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(Localization.text(label) + " " + Localization.text("vượt quá ")
                    + maxLength + Localization.text(" ký tự."));
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static UUID rowId(long id) {
        return new UUID(ROW_ID_PREFIX, id);
    }

    private static long numericId(UUID id) {
        if (id == null || id.getMostSignificantBits() != ROW_ID_PREFIX || id.getLeastSignificantBits() < 1) {
            throw new IllegalArgumentException("The selected record does not have a valid database identifier.");
        }
        return id.getLeastSignificantBits();
    }
}
