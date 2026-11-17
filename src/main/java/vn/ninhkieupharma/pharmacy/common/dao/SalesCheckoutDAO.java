package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.AllergyMatcher;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePricing;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SalesCheckoutDAO {
    private static final long ROW_ID_PREFIX = 0x4e4b504841524d41L;
    private static final long MAX_UNSIGNED_INT = 4_294_967_295L;
    private static final BigDecimal MAX_SALE_AMOUNT = new BigDecimal("9999999999.99");
    private static final BigDecimal MAX_SALE_QUANTITY = new BigDecimal("999999999.999");
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private record ProductState(
            long id, String name, boolean prescriptionRequired, BigDecimal price) {
    }

    private record Batch(long id, LocalDate expiry, BigDecimal quantity) {
    }

    private record CustomerState(long id, String name, long points) {
    }

    private record Promotion(
            long id, String code, BigDecimal discountPercent,
            LocalDateTime redemptionStart, LocalDateTime earningEnd, BigDecimal redemptionRate) {
    }

    private record LoyaltyPolicy(BigDecimal pointsPerThousand, BigDecimal currencyPerPoint) {
    }

    public List<SaleProduct> findSaleProducts() {
        String sql = """
                SELECT p.product_id, p.product_code, p.product_name, p.unit_name,
                       p.requires_prescription, p.selling_price,
                       COALESCE(SUM(b.quantity_on_hand), 0) AS stock_on_hand,
                       COALESCE((SELECT GROUP_CONCAT(pi.ingredient_name
                           ORDER BY pi.ingredient_name SEPARATOR '\\n')
                           FROM product_ingredients pi WHERE pi.product_id = p.product_id), '') AS ingredients
                FROM products p
                LEFT JOIN inventory_batches b ON b.product_id = p.product_id
                WHERE p.is_active = 1
                GROUP BY p.product_id, p.product_code, p.product_name, p.unit_name,
                         p.requires_prescription, p.selling_price
                ORDER BY p.product_name, p.product_id
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
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
                        ingredients));
            }
            return List.copyOf(products);
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể tải danh sách sản phẩm bán."), exception);
        }
    }

    public List<SaleAllergyWarning> findAllergyWarnings(
            Optional<UUID> customerRowId, List<UUID> productRowIds) {
        if (customerRowId == null || customerRowId.isEmpty() || productRowIds == null || productRowIds.isEmpty()) {
            return List.of();
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findAllergyWarnings(
                    connection, numericId(customerRowId.orElseThrow()),
                    productRowIds.stream().distinct().map(SalesCheckoutDAO::numericId).toList());
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể kiểm tra dị ứng của khách hàng."), exception);
        }
    }

    public SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        validateRequest(authenticatedUserId, request);
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                SaleReceipt receipt = completeSale(connection, authenticatedUserId, request);
                connection.commit();
                return receipt;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể ghi giao dịch bán hàng."), exception);
        }
    }

    private SaleReceipt completeSale(
            Connection connection, long authenticatedUserId, SaleOrderRequest request) throws SQLException {
        long employeeId = requireEmployee(connection, authenticatedUserId);
        Optional<CustomerState> customer = request.customerId()
                .map(id -> uncheckedCustomer(connection, numericId(id)));

        Map<Long, ProductState> products = lockProducts(connection, request.lines());
        boolean prescriptionRequired = request.lines().stream()
                .map(line -> products.get(numericId(line.productId())))
                .anyMatch(ProductState::prescriptionRequired);
        validatePrescription(request, prescriptionRequired);

        List<SaleAllergyWarning> warnings = customer.isEmpty()
                ? List.of()
                : findAllergyWarnings(connection, customer.orElseThrow().id(),
                        request.lines().stream().map(line -> numericId(line.productId())).distinct().toList());
        if (!warningKeys(warnings).equals(warningKeys(request.acknowledgedAllergyWarnings()))) {
            throw new IllegalStateException(Localization.text(
                    "Thông tin dị ứng của khách đã thay đổi. Hãy xem lại cảnh báo trước khi hoàn tất đơn."));
        }

        BigDecimal subtotal = ZERO_MONEY;
        List<BigDecimal> lineTotals = new ArrayList<>(request.lines().size());
        for (SaleLineRequest line : request.lines()) {
            ProductState product = products.get(numericId(line.productId()));
            BigDecimal lineTotal = line.quantity().multiply(product.price())
                    .setScale(2, RoundingMode.HALF_UP);
            requireSaleAmount(lineTotal);
            lineTotals.add(lineTotal);
            subtotal = subtotal.add(lineTotal);
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        requireSaleAmount(subtotal);

        LocalDateTime soldAt = LocalDateTime.now(APP_ZONE);
        Promotion promotion = findBestPromotion(connection, soldAt);
        LoyaltyPolicy loyalty = findLoyaltyPolicy(connection);
        BigDecimal redemptionRate = promotion != null
                && !soldAt.isBefore(promotion.redemptionStart())
                ? promotion.redemptionRate()
                : loyalty == null ? BigDecimal.ZERO : loyalty.currencyPerPoint();
        long pointsBalance = customer.map(CustomerState::points).orElse(0L);
        BigDecimal promotionPercent = promotion == null ? BigDecimal.ZERO : promotion.discountPercent();
        SalePricing.Result pricing = SalePricing.calculate(
                subtotal, promotionPercent, pointsBalance, request.redeemPoints(),
                redemptionRate, BigDecimal.ZERO, customer.isPresent());
        BigDecimal pointsRatePercent = findPointsRate(
                connection, promotion, soldAt, pricing.total(), loyalty);
        pricing = SalePricing.calculate(
                subtotal, promotionPercent, pointsBalance, request.redeemPoints(),
                redemptionRate, pointsRatePercent, customer.isPresent());
        subtotal = pricing.subtotal();
        BigDecimal promotionDiscount = pricing.promotionDiscount();
        BigDecimal pointsDiscount = pricing.pointsDiscount();
        BigDecimal discountAmount = pricing.totalDiscount();
        BigDecimal total = pricing.total();
        long pointsRedeemed = pricing.pointsRedeemed();
        long pointsEarned = pricing.pointsEarned();
        long remainingPoints = pointsBalance - pointsRedeemed + pointsEarned;
        if (remainingPoints < 0 || remainingPoints > MAX_UNSIGNED_INT
                || pointsEarned > MAX_UNSIGNED_INT || pointsRedeemed > MAX_UNSIGNED_INT) {
            throw new IllegalStateException(Localization.text(
                    "Điểm tích lũy vượt quá giới hạn dữ liệu được hỗ trợ."));
        }

        String saleCode = "HD" + soldAt.toLocalDate().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        long saleId = insertSale(connection, saleCode, customer.map(CustomerState::id).orElse(null),
                employeeId, promotion, soldAt, subtotal, promotionDiscount, pointsDiscount,
                discountAmount, total, pointsEarned, pointsRedeemed, request);
        for (int index = 0; index < request.lines().size(); index++) {
            SaleLineRequest line = request.lines().get(index);
            ProductState product = products.get(numericId(line.productId()));
            insertSaleLine(connection, saleId, product.id(), line, product.price(), lineTotals.get(index));
        }
        allocateInventory(connection, saleId, authenticatedUserId, request.lines(), products, soldAt);
        if (customer.isPresent()) {
            updateCustomerPoints(connection, customer.orElseThrow().id(), pointsBalance,
                    pointsRedeemed, pointsEarned);
        }
        return new SaleReceipt(
                saleCode, subtotal, promotionDiscount, pointsDiscount, total,
                pointsRedeemed, pointsEarned, remainingPoints,
                promotion == null ? "" : promotion.code());
    }

    private static long requireEmployee(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT e.employee_id
                FROM employees e
                JOIN users u ON u.user_id = e.user_id
                WHERE u.user_id = ? AND u.is_active = 1 AND e.employment_status = 'ACTIVE'
                """)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(Localization.text(
                            "Tài khoản đang đăng nhập chưa được liên kết với hồ sơ nhân viên đang hoạt động."));
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

    private static CustomerState uncheckedCustomer(Connection connection, long customerId) {
        try {
            return requireCustomer(connection, customerId);
        } catch (SQLException exception) {
            throw new DataAccessException(Localization.text("Không thể xác minh khách hàng đã chọn."), exception);
        }
    }

    private static Map<Long, ProductState> lockProducts(
            Connection connection, List<SaleLineRequest> lines) throws SQLException {
        List<Long> productIds = lines.stream().map(line -> numericId(line.productId()))
                .distinct().sorted().toList();
        Map<Long, ProductState> products = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT product_id, product_name, requires_prescription, selling_price
                FROM products
                WHERE product_id = ? AND is_active = 1
                FOR UPDATE
                """)) {
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
                            resultSet.getBoolean("requires_prescription"), price));
                }
            }
        }
        return products;
    }

    private static void validatePrescription(SaleOrderRequest request, boolean required) {
        if (!required) {
            return;
        }
        if (blank(request.prescriptionCode()) || request.prescriptionDate() == null
                || request.prescriptionDate().isAfter(LocalDate.now(APP_ZONE))
                || blank(request.prescribingDoctor())) {
            throw new IllegalArgumentException(Localization.text(
                    "Thuốc kê đơn yêu cầu mã toa, ngày kê hợp lệ và tên bác sĩ kê đơn."));
        }
    }

    private static Promotion findBestPromotion(Connection connection, LocalDateTime now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT promotion_id, promotion_code, discount_percent,
                       points_redemption_starts_at, points_earning_ends_at, redemption_vnd_per_point
                FROM promotions
                WHERE is_active = 1 AND discount_percent > 0
                  AND starts_at <= ? AND ends_at >= ?
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
            Connection connection, String saleCode, Long customerId, long employeeId,
            Promotion promotion, LocalDateTime soldAt, BigDecimal subtotal,
            BigDecimal promotionDiscount, BigDecimal pointsDiscount, BigDecimal discountAmount,
            BigDecimal total, long pointsEarned, long pointsRedeemed, SaleOrderRequest request)
            throws SQLException {
        String sql = """
                INSERT INTO sales
                    (sale_code, customer_id, employee_id, promotion_id, sold_at, subtotal,
                     promotion_discount_amount, points_discount_amount, discount_amount, total_amount,
                     points_earned, points_redeemed, status, prescription_code, prescription_date,
                     prescribing_doctor, clinic, diagnosis, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, saleCode);
            setNullableLong(statement, 2, customerId);
            statement.setLong(3, employeeId);
            setNullableLong(statement, 4, promotion == null ? null : promotion.id());
            statement.setObject(5, soldAt);
            statement.setBigDecimal(6, subtotal);
            statement.setBigDecimal(7, promotionDiscount);
            statement.setBigDecimal(8, pointsDiscount);
            statement.setBigDecimal(9, discountAmount);
            statement.setBigDecimal(10, total);
            statement.setLong(11, pointsEarned);
            statement.setLong(12, pointsRedeemed);
            setNullableString(statement, 13, request.prescriptionCode());
            if (request.prescriptionDate() == null) {
                statement.setNull(14, Types.DATE);
            } else {
                statement.setDate(14, java.sql.Date.valueOf(request.prescriptionDate()));
            }
            setNullableString(statement, 15, request.prescribingDoctor());
            setNullableString(statement, 16, request.clinic());
            setNullableString(statement, 17, request.diagnosis());
            setNullableString(statement, 18, request.note());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL did not return the generated sale id.");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void insertSaleLine(
            Connection connection, long saleId, long productId, SaleLineRequest line,
            BigDecimal price, BigDecimal total) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO sale_items
                    (sale_id, product_id, quantity, unit_price, line_discount, line_total,
                     dosage_instructions, treatment_days)
                VALUES (?, ?, ?, ?, 0, ?, ?, ?)
                """)) {
            statement.setLong(1, saleId);
            statement.setLong(2, productId);
            statement.setBigDecimal(3, line.quantity());
            statement.setBigDecimal(4, price);
            statement.setBigDecimal(5, total);
            setNullableString(statement, 6, line.dosageInstructions());
            statement.setInt(7, line.treatmentDays());
            statement.executeUpdate();
        }
    }

    private static void allocateInventory(
            Connection connection, long saleId, long createdByUserId, List<SaleLineRequest> lines,
            Map<Long, ProductState> products, LocalDateTime soldAt) throws SQLException {
        List<SaleLineRequest> allocationOrder = lines.stream()
                .sorted(Comparator.comparingInt(SaleLineRequest::treatmentDays).reversed()
                        .thenComparing(line -> numericId(line.productId())))
                .toList();
        for (SaleLineRequest line : allocationOrder) {
            long productId = numericId(line.productId());
            LocalDate lastTreatmentDate = soldAt.toLocalDate().plusDays(line.treatmentDays() - 1L);
            List<Batch> batches = lockEligibleBatches(connection, productId, lastTreatmentDate);
            BigDecimal remaining = line.quantity();
            for (Batch batch : batches) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal allocated = remaining.min(batch.quantity());
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE inventory_batches
                        SET quantity_on_hand = quantity_on_hand - ?
                        WHERE batch_id = ? AND quantity_on_hand >= ?
                        """)) {
                    update.setBigDecimal(1, allocated);
                    update.setLong(2, batch.id());
                    update.setBigDecimal(3, allocated);
                    if (update.executeUpdate() != 1) {
                        throw new SQLException("Inventory changed while the sale was being completed.");
                    }
                }
                insertSaleMovement(connection, batch.id(), createdByUserId, saleId, allocated,
                        products.get(productId).name(), soldAt);
                remaining = remaining.subtract(allocated);
            }
            if (remaining.signum() > 0) {
                throw new IllegalStateException(Localization.text("Không đủ tồn kho chưa hết hạn cho ")
                        + products.get(productId).name()
                        + Localization.text(" đến hết thời gian điều trị đã khai báo."));
            }
        }
    }

    private static List<Batch> lockEligibleBatches(
            Connection connection, long productId, LocalDate lastTreatmentDate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT batch_id, expiry_date, quantity_on_hand
                FROM inventory_batches
                WHERE product_id = ? AND expiry_date >= ? AND quantity_on_hand > 0
                ORDER BY expiry_date, batch_id
                FOR UPDATE
                """)) {
            statement.setLong(1, productId);
            statement.setDate(2, java.sql.Date.valueOf(lastTreatmentDate));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Batch> batches = new ArrayList<>();
                while (resultSet.next()) {
                    batches.add(new Batch(
                            resultSet.getLong("batch_id"),
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

    private static void validateRequest(long authenticatedUserId, SaleOrderRequest request) {
        if (authenticatedUserId < 1) {
            throw new IllegalArgumentException(Localization.text(
                    "Cần có tài khoản nhân viên đăng nhập để hoàn tất đơn."));
        }
        if (request == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException(Localization.text(
                    "Vui lòng thêm ít nhất một sản phẩm vào giỏ."));
        }
        for (SaleLineRequest line : request.lines()) {
            if (line == null || line.productId() == null || line.quantity() == null
                    || line.quantity().signum() <= 0 || line.quantity().scale() > 3
                    || line.treatmentDays() < 1 || line.treatmentDays() > 3650) {
                throw new IllegalArgumentException(Localization.text(
                        "Mỗi dòng cần có sản phẩm, số lượng dương và thời gian điều trị từ 1 đến 3650 ngày."));
            }
            numericId(line.productId());
            if (line.quantity().precision() > 12) {
                throw new IllegalArgumentException(Localization.text(
                        "Số lượng bán vượt quá phạm vi được hỗ trợ."));
            }
            if (line.quantity().compareTo(MAX_SALE_QUANTITY) > 0) {
                throw new IllegalArgumentException(Localization.text(
                        "Số lượng bán vượt quá phạm vi được hỗ trợ."));
            }
            requireMaxLength(line.dosageInstructions(), 255, "Hướng dẫn sử dụng");
        }
        requireMaxLength(request.prescriptionCode(), 80, "Mã toa thuốc");
        requireMaxLength(request.prescribingDoctor(), 120, "Tên bác sĩ");
        requireMaxLength(request.clinic(), 180, "Tên cơ sở khám");
        requireMaxLength(request.diagnosis(), 255, "Chẩn đoán");
        requireMaxLength(request.note(), 500, "Ghi chú đơn hàng");
    }

    private static void requireSaleAmount(BigDecimal amount) {
        if (amount.compareTo(MAX_SALE_AMOUNT) > 0) {
            throw new IllegalArgumentException(Localization.text(
                    "Giá trị đơn hàng vượt quá phạm vi dữ liệu được hỗ trợ."));
        }
    }

    private static void requireMaxLength(String value, int maxLength, String label) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(Localization.text(label + " vượt quá ")
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
