package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.auth.PasswordHasher;
import vn.ninhkieupharma.pharmacy.common.auth.User;
import vn.ninhkieupharma.pharmacy.common.auth.UserRole;
import vn.ninhkieupharma.pharmacy.common.dao.InventoryWorkflowDAO;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleLineRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleStockShortage;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SalePendingOrder;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleQuote;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;
import vn.ninhkieupharma.pharmacy.domain.sales.SalesReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Maps the feature screens to the normalized tables in 01_schema_quan_ly_cua_hang.sql.
 * The application deliberately does not own schema creation or demo business data.
 */
public final class RelationalFeatureModuleDAO implements FeatureModuleDAO, InventoryWorkflowDAO {
    private static final long ROW_ID_PREFIX = 0x4e4b504841524d41L;
    private final SalesCheckoutDAO salesCheckoutDAO = new SalesCheckoutDAO();
    private final UserRole actorRole;

    /** Creates a repository without access to administrative sales operations. */
    public RelationalFeatureModuleDAO() {
        actorRole = null;
    }

    public RelationalFeatureModuleDAO(User authenticatedUser) {
        Objects.requireNonNull(authenticatedUser);
        if (!authenticatedUser.active() || authenticatedUser.id() < 1) {
            throw new IllegalArgumentException("An active user is required.");
        }
        actorRole = Objects.requireNonNull(authenticatedUser.role());
    }

    private void requireSalesAdministrationAccess(String moduleId) {
        if ("UC-18".equals(moduleId) || "UC-19".equals(moduleId)) {
            requireAdministrator();
        }
    }

    private void requireAdministrator() {
        if (actorRole != UserRole.ADMIN) {
            throw new SecurityException(Localization.text("Chức năng này chỉ dành cho quản trị viên."));
        }
    }

    @Override
    public boolean supportsNativeReports() {
        return true;
    }

    @Override
    public List<ModuleRow> findAll(String moduleId) {
        requireSalesAdministrationAccess(moduleId);
        return switch (moduleId) {
            case "UC-01" -> query(moduleId, """
                    SELECT e.employee_id, e.employee_code, e.full_name, COALESCE(e.phone, ''),
                           COALESCE(e.email, ''), COALESCE(u.username, 'Chưa có tài khoản'),
                           CASE r.role_code WHEN 'ADMIN' THEN 'Quản trị viên'
                                WHEN 'STAFF' THEN 'Nhân viên' ELSE 'Chưa có tài khoản' END,
                           CASE WHEN u.user_id IS NULL THEN 'Chưa có tài khoản'
                                WHEN u.is_active THEN 'Đang hoạt động' ELSE 'Ngừng hoạt động' END
                    FROM employees e
                    LEFT JOIN users u ON u.user_id = e.user_id
                    LEFT JOIN roles r ON r.role_id = u.role_id
                    ORDER BY e.employee_id
                    """);
            case "UC-02" -> query(moduleId, """
                    SELECT a.availability_id, e.full_name, a.available_date, a.start_time, a.end_time,
                           COALESCE(a.note, '')
                    FROM employee_availabilities a
                    JOIN employees e ON e.employee_id = a.employee_id
                    ORDER BY a.available_date, a.start_time
                    """);
            case "UC-03" -> query(moduleId, """
                    SELECT a.assignment_id, a.work_date, s.shift_name, e.full_name, a.status,
                           COALESCE(a.note, '')
                    FROM schedule_assignments a
                    JOIN employees e ON e.employee_id = a.employee_id
                    JOIN shifts s ON s.shift_id = a.shift_id
                    ORDER BY a.work_date, s.start_time, e.full_name
                    """);
            case "UC-04" -> query(moduleId, """
                    SELECT ar.attendance_id, e.full_name, s.shift_name,
                           COALESCE(DATE_FORMAT(ar.check_in_at, '%Y-%m-%d %H:%i'), ''),
                           COALESCE(DATE_FORMAT(ar.check_out_at, '%Y-%m-%d %H:%i'), ''),
                           COALESCE(ar.note, '')
                    FROM attendance_records ar
                    JOIN schedule_assignments a ON a.assignment_id = ar.assignment_id
                    JOIN employees e ON e.employee_id = a.employee_id
                    JOIN shifts s ON s.shift_id = a.shift_id
                    ORDER BY ar.check_in_at DESC, ar.attendance_id DESC
                    """);
            case "UC-05" -> query(moduleId, """
                    SELECT e.employee_id,
                           e.full_name,
                           'All time',
                           CONCAT(
                               SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END),
                               '/',
                               COUNT(DISTINCT a.assignment_id),
                               ' ca (Phủ ',
                               COALESCE(ROUND(
                                   SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) * 100.0
                                   / NULLIF(COUNT(DISTINCT a.assignment_id), 0), 1
                               ), 0),
                               '%)'
                           ),
                           COALESCE(ROUND(SUM(
                               CASE WHEN ar.check_in_at IS NOT NULL AND ar.check_out_at IS NOT NULL
                                    THEN GREATEST(0, TIMESTAMPDIFF(MINUTE, ar.check_in_at, ar.check_out_at) - s.break_minutes) / 60.0
                                    ELSE 0 END), 2), 0),
                           CASE WHEN COUNT(DISTINCT a.assignment_id) = 0 THEN 'Chưa phân ca'
                                ELSE 'Đã tổng hợp từ chấm công' END
                    FROM employees e
                    LEFT JOIN schedule_assignments a ON a.employee_id = e.employee_id AND a.status <> 'CANCELLED'
                    LEFT JOIN shifts s ON s.shift_id = a.shift_id
                    LEFT JOIN attendance_records ar ON ar.assignment_id = a.assignment_id
                    GROUP BY e.employee_id, e.full_name
                    ORDER BY e.full_name
                    """);
            case "UC-06" -> query(moduleId, """
                    SELECT a.assignment_id,
                           e.full_name,
                           CONCAT(s.shift_name, ' (', a.work_date, ')'),
                           CONCAT(TIME_FORMAT(s.start_time, '%H:%i'), ' - ', TIME_FORMAT(s.end_time, '%H:%i')),
                           CONCAT(
                               COALESCE(DATE_FORMAT(ar.check_in_at, '%H:%i'), 'Chưa vào'),
                               ' - ',
                               COALESCE(DATE_FORMAT(ar.check_out_at, '%H:%i'), 'Chưa ra')
                           ),
                           CASE
                               WHEN ar.check_in_at IS NULL THEN 'Vắng mặt / Chưa chấm công'
                               WHEN TIME(ar.check_in_at) > s.start_time
                                    THEN CONCAT('Đi muộn ', TIMESTAMPDIFF(MINUTE, s.start_time, TIME(ar.check_in_at)), ' phút')
                               WHEN ar.check_out_at IS NOT NULL AND TIME(ar.check_out_at) < s.end_time
                                    THEN CONCAT('Về sớm ', TIMESTAMPDIFF(MINUTE, TIME(ar.check_out_at), s.end_time), ' phút')
                               ELSE 'Đúng giờ'
                           END
                    FROM schedule_assignments a
                    JOIN employees e ON e.employee_id = a.employee_id
                    JOIN shifts s ON s.shift_id = a.shift_id
                    LEFT JOIN attendance_records ar ON ar.assignment_id = a.assignment_id
                    WHERE a.status <> 'CANCELLED'
                    ORDER BY a.work_date DESC, s.start_time ASC, e.full_name ASC
                    """);
           case "UC-07" -> query(moduleId, """
                    SELECT category_id, category_name, COALESCE(description, ''),
                           IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM product_categories ORDER BY category_id
                    """);
           case "UC-08" -> query(moduleId, """
                    SELECT p.product_id AS row_key,
                           p.product_code,
                           p.product_name,
                           c.category_name,
                           COALESCE(p.dosage_form, ''),
                           p.unit_name,
                           CAST(COALESCE(p.selling_price, 0) AS CHAR),
                           CASE WHEN p.requires_prescription = 1 THEN 'Yes' ELSE 'No' END,
                           CASE WHEN p.is_active = 1 THEN 'ACTIVE' ELSE 'INACTIVE' END,
                           COALESCE(GROUP_CONCAT(DISTINCT s.supplier_name ORDER BY s.supplier_name SEPARATOR ', '), '')
                    FROM products p
                    JOIN product_categories c ON c.category_id = p.category_id
                    LEFT JOIN supplier_products sp ON sp.product_id = p.product_id
                    LEFT JOIN suppliers s ON s.supplier_id = sp.supplier_id
                    GROUP BY p.product_id, p.product_code, p.product_name, c.category_name,
                             p.dosage_form, p.unit_name, p.selling_price, p.requires_prescription, p.is_active
                    ORDER BY p.product_code
                    """);
            case "UC-09" -> query(moduleId, """
                    SELECT supplier_id, supplier_code, supplier_name, COALESCE(phone, ''),
                           COALESCE(email, ''), COALESCE(address, ''), IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM suppliers ORDER BY supplier_id
                    """);
            case "UC-10" -> query(moduleId, """
                    WITH quarters AS (
                        SELECT YEAR(created_at) AS year_num, QUARTER(created_at) AS quarter_num
                        FROM products
                        UNION
                        SELECT YEAR(CURRENT_DATE), QUARTER(CURRENT_DATE)
                    ),
                    quarter_bounds AS (
                        SELECT year_num, quarter_num,
                               LAST_DAY(STR_TO_DATE(
                                   CONCAT(year_num, '-', quarter_num * 3, '-01'), '%Y-%m-%d')) AS quarter_end
                        FROM quarters
                    ),
                    category_quarter AS (
                        SELECT qb.year_num, qb.quarter_num, c.category_id, c.category_name,
                               COUNT(DISTINCT p.product_id) AS product_count,
                               COUNT(DISTINCT COALESCE(NULLIF(p.dosage_form, ''), 'Mặc định')) AS dosage_count,
                               COUNT(DISTINCT sp.supplier_id) AS supplier_count
                        FROM quarter_bounds qb
                        JOIN products p ON DATE(p.created_at) <= qb.quarter_end AND p.is_active = 1
                        JOIN product_categories c ON c.category_id = p.category_id
                        LEFT JOIN supplier_products sp ON sp.product_id = p.product_id
                        GROUP BY qb.year_num, qb.quarter_num, c.category_id, c.category_name
                    ),
                    quarter_totals AS (
                        SELECT year_num, quarter_num, SUM(product_count) AS total_product_count
                        FROM category_quarter
                        GROUP BY year_num, quarter_num
                    )
                    SELECT ROW_NUMBER() OVER (
                               ORDER BY cq.year_num DESC, cq.quarter_num DESC,
                                        cq.product_count DESC, cq.category_id
                           ) AS row_key,
                           CONCAT(cq.year_num, '-Q', cq.quarter_num) AS quarter_label,
                           cq.category_name,
                           CONCAT(cq.product_count, ' SP (', cq.dosage_count, ' dạng / ',
                                  cq.supplier_count, ' NCC)'),
                           CONCAT(ROUND(100.0 * cq.product_count /
                               NULLIF(qt.total_product_count, 0), 1), '%'),
                           CASE
                               WHEN cq.product_count >= 5 AND cq.supplier_count >= 2
                                   THEN 'Nhiều lựa chọn thay thế'
                               WHEN cq.product_count >= 2
                                   THEN 'Có thể thay thế cơ bản'
                               ELSE 'Ít mặt hàng - Cần mở rộng'
                           END
                    FROM category_quarter cq
                    JOIN quarter_totals qt
                      ON qt.year_num = cq.year_num AND qt.quarter_num = cq.quarter_num
                    ORDER BY cq.year_num DESC, cq.quarter_num DESC, cq.product_count DESC
                    """);
            case "UC-11" -> query(moduleId, """
                    WITH supplier_month_products AS (
                        SELECT DATE_FORMAT(gr.received_at, '%Y-%m') AS month_label,
                               gr.supplier_id,
                               ib.product_id
                        FROM goods_receipts gr
                        JOIN goods_receipt_items gri ON gri.receipt_id = gr.receipt_id
                        JOIN inventory_batches ib ON ib.batch_id = gri.batch_id
                        WHERE gr.status = 'RECEIVED'
                        GROUP BY DATE_FORMAT(gr.received_at, '%Y-%m'), gr.supplier_id, ib.product_id
                    ),
                    month_totals AS (
                        SELECT month_label, COUNT(DISTINCT product_id) AS total_product_count
                        FROM supplier_month_products
                        GROUP BY month_label
                    ),
                    supplier_month_totals AS (
                        SELECT month_label, supplier_id, COUNT(*) AS product_count
                        FROM supplier_month_products
                        GROUP BY month_label, supplier_id
                    )
                    SELECT ROW_NUMBER() OVER (
                               ORDER BY smt.month_label DESC, smt.product_count DESC, smt.supplier_id
                           ) AS row_key,
                           smt.month_label,
                           s.supplier_name,
                           smt.product_count AS item_count,
                           CONCAT(ROUND(100.0 * smt.product_count /
                               NULLIF(mt.total_product_count, 0), 1), '%'),
                           CASE
                               WHEN 1.0 * smt.product_count /
                                   NULLIF(mt.total_product_count, 0) >= 0.50
                                   THEN 'Phụ thuộc lớn (>= 50%)'
                               WHEN 1.0 * smt.product_count /
                                   NULLIF(mt.total_product_count, 0) >= 0.25
                                   THEN 'Tỷ trọng trung bình'
                               ELSE 'Phân bổ đều / Thấp'
                           END
                    FROM supplier_month_totals smt
                    JOIN month_totals mt ON mt.month_label = smt.month_label
                    JOIN suppliers s ON s.supplier_id = smt.supplier_id
                    ORDER BY smt.month_label DESC, smt.product_count DESC
                    """);
            case "UC-12" -> query(moduleId, """
                    SELECT gr.receipt_id, gr.receipt_code, s.supplier_name,
                           DATE_FORMAT(gr.received_at, '%Y-%m-%d'),
                           COUNT(gri.receipt_item_id),
                           CASE WHEN gr.status = 'RECEIVED' THEN 'Đã nhận' ELSE 'Đã hủy' END
                    FROM goods_receipts gr
                    JOIN suppliers s ON s.supplier_id = gr.supplier_id
                    LEFT JOIN goods_receipt_items gri ON gri.receipt_id = gr.receipt_id
                    GROUP BY gr.receipt_id, gr.receipt_code, s.supplier_name, gr.received_at, gr.status
                    ORDER BY gr.received_at DESC
                    """);
            case "UC-13" -> query(moduleId, """
                    SELECT location_id, location_code, location_name, COALESCE(description, ''),
                           IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM warehouse_locations ORDER BY location_code
                    """);
            case "UC-14" -> query(moduleId, """
                    SELECT st.stocktake_id, CONCAT('ST-', st.stocktake_id), wl.location_name,
                           st.stocktake_date, COALESCE(SUM(si.difference_quantity), 0), st.status
                    FROM stocktakes st
                    JOIN warehouse_locations wl ON wl.location_id = st.location_id
                    LEFT JOIN stocktake_items si ON si.stocktake_id = st.stocktake_id
                    GROUP BY st.stocktake_id, wl.location_name, st.stocktake_date, st.status, st.note
                    ORDER BY st.stocktake_date DESC
                    """);
            case "UC-15" -> query(moduleId, """
                    SELECT im.movement_id, DATE_FORMAT(im.moved_at, '%Y-%m-%d %H:%i'),
                           CONCAT(p.product_name, ' / ', b.batch_code),
                           im.movement_type, CAST(im.quantity_delta AS CHAR),
                           COALESCE(im.note, ''),
                           COALESCE(DATE_FORMAT(b.expiry_date, '%Y-%m-%d'), '—'),
                           CONCAT(wl.location_code, ' · ', wl.location_name)
                    FROM inventory_movements im
                    JOIN inventory_batches b ON b.batch_id = im.batch_id
                    JOIN products p ON p.product_id = b.product_id
                    JOIN warehouse_locations wl ON wl.location_id = b.location_id
                    ORDER BY im.moved_at DESC
                    """);
            case "UC-16" -> query(moduleId, """
                    SELECT c.customer_id, c.customer_code, c.full_name, COALESCE(c.phone, ''),
                           COALESCE(c.email, ''),
                           COALESCE((SELECT GROUP_CONCAT(CONCAT(ca.allergen_name,
                               IF(ca.note IS NULL OR ca.note = '', '', CONCAT(' | ', ca.note)))
                               ORDER BY ca.allergen_name SEPARATOR '\\n')
                               FROM customer_allergies ca WHERE ca.customer_id = c.customer_id), ''),
                           c.loyalty_points,
                           CASE
                               WHEN c.is_active = 0 AND c.is_blocked = 1 THEN 'ARCHIVED_BLOCKED'
                               WHEN c.is_active = 0 THEN 'ARCHIVED'
                               WHEN c.is_blocked = 1 THEN 'BLOCKED'
                               ELSE 'ACTIVE'
                           END,
                           COALESCE(c.block_reason, ''),
                           COALESCE(DATE_FORMAT(c.date_of_birth, '%Y-%m-%d'), '')
                    FROM customers c ORDER BY c.customer_id
                    """);
            case "UC-17" -> query(moduleId, """
                    SELECT s.sale_id, s.sale_code, DATE_FORMAT(s.sold_at, '%Y-%m-%d %H:%i'),
                           COALESCE(c.full_name, 'Walk-in'), e.full_name,
                           CAST(ROUND(s.total_amount, 0) AS UNSIGNED), s.status
                    FROM sales s
                    LEFT JOIN customers c ON c.customer_id = s.customer_id
                    JOIN employees e ON e.employee_id = s.employee_id
                    ORDER BY s.sold_at DESC
                    """);
            case "UC-18" -> findPromotions();
            case "UC-19" -> findCompletedSalesByDay(null, null).stream()
                    .map(day -> row(day.start().toEpochDay(), List.of(
                            day.start().toString(),
                            Long.toString(day.totals().orderCount()),
                            day.totals().revenue().toPlainString(),
                            day.totals().discount().toPlainString(),
                            day.totals().averageOrderValue().toPlainString())))
                    .toList();
            default -> throw new IllegalArgumentException("Unknown feature module: " + moduleId);
        };
    }

    @Override
    public List<SalesReport.Row> findCompletedSalesByDay(
            LocalDate fromInclusive, LocalDate toExclusive) {
        requireAdministrator();
        boolean limited = fromInclusive != null && toExclusive != null;
        if (!limited && (fromInclusive != null || toExclusive != null)) {
            throw new IllegalArgumentException("Both report date boundaries are required.");
        }
        String sql = """
                SELECT DATE(sold_at), COUNT(*), SUM(total_amount), SUM(discount_amount)
                FROM sales
                WHERE status = 'COMPLETED'
                """ + (limited ? " AND sold_at >= ? AND sold_at < ? " : "") + """
                GROUP BY DATE(sold_at)
                ORDER BY DATE(sold_at)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (limited) {
                statement.setObject(1, fromInclusive.atStartOfDay());
                statement.setObject(2, toExclusive.atStartOfDay());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SalesReport.Row> rows = new ArrayList<>();
                while (resultSet.next()) {
                    LocalDate day = resultSet.getObject(1, LocalDate.class);
                    SalesReport.Totals totals = new SalesReport.Totals(resultSet.getLong(2),
                            resultSet.getBigDecimal(3), resultSet.getBigDecimal(4));
                    rows.add(new SalesReport.Row(day, totals));
                }
                return List.copyOf(rows);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Không thể tải báo cáo doanh thu.", exception);
        }
    }

    @Override
    public List<ModuleRow> findCustomerPurchaseHistory(UUID customerId) {
        long id = numericId(customerId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT sale_id, sale_code, DATE_FORMAT(sold_at, '%d/%m/%Y %H:%i'),
                            CAST(total_amount AS CHAR), status
                     FROM sales
                     WHERE customer_id = ?
                     ORDER BY sold_at DESC, sale_id DESC
                     """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ModuleRow> history = new ArrayList<>();
                while (resultSet.next()) {
                    history.add(row(resultSet.getLong("sale_id"), List.of(
                            resultSet.getString("sale_code"),
                            resultSet.getString(3),
                            resultSet.getString(4),
                            resultSet.getString("status"))));
                }
                return List.copyOf(history);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load customer purchase history.", exception);
        }
    }

    @Override
    public List<ModuleRow> findCustomerLoyaltyHistory(UUID customerId) {
        long id = numericId(customerId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                    SELECT sale_id, sale_code, DATE_FORMAT(sold_at, '%d/%m/%Y %H:%i'),
                           points_earned, points_redeemed
                    FROM sales
                    WHERE customer_id = ? AND status = 'COMPLETED'
                      AND (points_earned > 0 OR points_redeemed > 0)
                    ORDER BY sold_at DESC, sale_id DESC
                    """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ModuleRow> history = new ArrayList<>();
                while (resultSet.next()) {
                    long saleId = resultSet.getLong("sale_id");
                    String saleCode = resultSet.getString("sale_code");
                    String date = resultSet.getString(3);
                    long earned = resultSet.getLong("points_earned");
                    long redeemed = resultSet.getLong("points_redeemed");
                    if (earned > 0) {
                        history.add(row(loyaltyEventRowId(saleId, false),
                                List.of(saleCode, date, "Tích điểm", Long.toString(earned))));
                    }
                    if (redeemed > 0) {
                        history.add(new ModuleRow(loyaltyEventRowId(saleId, true),
                                List.of(saleCode, date, "Đổi điểm", Long.toString(-redeemed))));
                    }
                }
                return List.copyOf(history);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Không thể tải lịch sử điểm của khách hàng.", exception);
        }
    }

    @Override
    public List<ModuleRow> findCustomersAvailableForSale() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT c.customer_id, c.full_name, COALESCE(c.phone, '') AS phone,
                            COALESCE(c.email, '') AS email,
                            COALESCE((SELECT GROUP_CONCAT(CONCAT(ca.allergen_name,
                                IF(ca.note IS NULL OR ca.note = '', '', CONCAT(' | ', ca.note)))
                                ORDER BY ca.allergen_name SEPARATOR '\\n')
                                FROM customer_allergies ca WHERE ca.customer_id = c.customer_id), '') AS allergies,
                            c.loyalty_points,
                            COALESCE(DATE_FORMAT(c.date_of_birth, '%Y-%m-%d'), '') AS date_of_birth
                     FROM customers c
                     WHERE c.is_active = 1 AND c.is_blocked = 0
                     ORDER BY c.full_name, c.customer_id
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            List<ModuleRow> customers = new ArrayList<>();
            while (resultSet.next()) {
                customers.add(row(resultSet.getLong("customer_id"), List.of(
                        resultSet.getString("full_name"),
                        resultSet.getString("phone"),
                        resultSet.getString("email"),
                        resultSet.getString("allergies"),
                        Long.toString(resultSet.getLong("loyalty_points")),
                        resultSet.getString("date_of_birth"))));
            }
            return List.copyOf(customers);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load customers available for sale.", exception);
        }
    }

    @Override
    public ModuleRow createCustomerFromSale(List<String> values) {
        requireSize("Sales customer", values, 5);
        List<String> customerValues = List.of(
                "", values.get(0), values.get(1), values.get(2), values.get(3),
                "0", "ACTIVE", "", values.get(4));
        ModuleRow customer = createCustomer(customerValues);
        return row(customer.id().getLeastSignificantBits(), List.of(
                values.get(0), values.get(1), values.get(2), values.get(3), "0", values.get(4)));
    }

    @Override
    public ModuleRow updateCustomerFromSale(UUID customerId, List<String> values) {
        requireSize("Sales customer", values, 5);
        long id = numericId(customerId);
        validateCustomer(List.of(
                "", values.get(0), values.get(1), values.get(2), values.get(3),
                "0", "ACTIVE", "", values.get(4)));
        List<CustomerAllergy> allergies = parseCustomerAllergies(values.get(3));
        return transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE customers
                    SET full_name = ?, phone = ?, email = ?, date_of_birth = ?
                    WHERE customer_id = ? AND is_active = 1 AND is_blocked = 0
                    """)) {
                statement.setString(1, values.get(0).trim());
                statement.setString(2, blankToNull(values.get(1)));
                statement.setString(3, blankToNull(values.get(2)));
                setCustomerDate(statement, 4, values.get(4));
                statement.setLong(5, id);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalStateException("Khách hàng không còn đủ điều kiện cập nhật tại quầy.");
                }
            }
            replaceAllergies(connection, id, allergies);
            long points;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT loyalty_points FROM customers WHERE customer_id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("Không tìm thấy hồ sơ khách hàng.");
                    }
                    points = resultSet.getLong(1);
                }
            }
            return row(id, List.of(
                    values.get(0), values.get(1), values.get(2), values.get(3),
                    Long.toString(points), values.get(4)));
        });
    }

    @Override
    public List<SaleProduct> findSaleProducts() {
        return salesCheckoutDAO.findSaleProducts();
    }

    @Override
    public List<SaleStockShortage> previewSaleStock(List<SaleLineRequest> lines) {
        return salesCheckoutDAO.previewSaleStock(lines);
    }

    @Override
    public List<SaleAllergyWarning> findSaleAllergyWarnings(
            Optional<UUID> customerId, List<UUID> productIds) {
        return salesCheckoutDAO.findAllergyWarnings(customerId, productIds);
    }

    @Override
    public SaleReceipt completeSale(long authenticatedUserId, SaleOrderRequest request) {
        return salesCheckoutDAO.completeSale(authenticatedUserId, request);
    }

    @Override
    public SalePendingOrder createPendingSale(long authenticatedUserId, SaleOrderRequest request) {
        return salesCheckoutDAO.createPendingSale(authenticatedUserId, request);
    }

    @Override
    public SalePendingOrder updatePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request) {
        return salesCheckoutDAO.updatePendingSale(authenticatedUserId, saleId, revision, request);
    }

    @Override
    public SalePendingOrder findPendingSale(UUID saleId) {
        return salesCheckoutDAO.findPendingSale(saleId);
    }

    @Override
    public SaleQuote quotePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request) {
        return salesCheckoutDAO.quotePendingSale(authenticatedUserId, saleId, revision, request);
    }

    @Override
    public SaleReceipt completePendingSale(
            long authenticatedUserId, UUID saleId, long revision, SaleOrderRequest request, SaleQuote acceptedQuote) {
        return salesCheckoutDAO.completePendingSale(authenticatedUserId, saleId, revision, request, acceptedQuote);
    }

    @Override
    public void cancelPendingSale(long authenticatedUserId, UUID saleId, long revision) {
        salesCheckoutDAO.cancelPendingSale(authenticatedUserId, saleId, revision);
    }

    @Override
    public List<ModuleRow> findSaleDetails(UUID saleId) {
        return salesCheckoutDAO.findSaleDetails(saleId);
    }

    @Override
    public List<SupplierOption> findInventorySuppliers() {
        return queryOptions(
                "SELECT supplier_id, supplier_code, supplier_name FROM suppliers "
                        + "WHERE is_active = 1 ORDER BY supplier_name",
                resultSet -> new SupplierOption(
                        resultSet.getLong("supplier_id"),
                        resultSet.getString("supplier_code"),
                        resultSet.getString("supplier_name")));
    }

    @Override
    public List<ProductOption> findInventoryProducts() {
        return queryOptions(
                "SELECT product_id, product_code, product_name, unit_name FROM products "
                        + "WHERE is_active = 1 ORDER BY product_name, product_code",
                resultSet -> new ProductOption(
                        resultSet.getLong("product_id"),
                        resultSet.getString("product_code"),
                        resultSet.getString("product_name"),
                        resultSet.getString("unit_name")));
    }

    @Override
    public List<LocationOption> findInventoryLocations() {
        return queryOptions(
                "SELECT location_id, location_code, location_name FROM warehouse_locations "
                        + "WHERE is_active = 1 ORDER BY location_code",
                resultSet -> new LocationOption(
                        resultSet.getLong("location_id"),
                        resultSet.getString("location_code"),
                        resultSet.getString("location_name")));
    }

    @Override
    public List<BatchOption> findInventoryBatches(Long locationId) {
        String sql = """
                SELECT b.batch_id, p.product_id, b.location_id, p.product_code, p.product_name,
                       b.batch_code, b.expiry_date, b.quantity_on_hand,
                       wl.location_code, wl.location_name
                FROM inventory_batches b
                JOIN products p ON p.product_id = b.product_id
                JOIN warehouse_locations wl ON wl.location_id = b.location_id
                """ + (locationId == null ? "" : "WHERE b.location_id = ? ")
                + "ORDER BY p.product_code, b.expiry_date, b.batch_code";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (locationId != null) {
                statement.setLong(1, locationId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<BatchOption> batches = new ArrayList<>();
                while (resultSet.next()) {
                    java.sql.Date expiry = resultSet.getDate("expiry_date");
                    batches.add(new BatchOption(
                            resultSet.getLong("batch_id"),
                            resultSet.getLong("product_id"),
                            resultSet.getLong("location_id"),
                            resultSet.getString("product_code"),
                            resultSet.getString("product_name"),
                            resultSet.getString("batch_code"),
                            expiry == null ? null : expiry.toLocalDate(),
                            resultSet.getBigDecimal("quantity_on_hand"),
                            resultSet.getString("location_code"),
                            resultSet.getString("location_name")));
                }
                return List.copyOf(batches);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load inventory batches.", exception);
        }
    }

    @Override
    public List<ReceiptDetail> findGoodsReceiptDetails(UUID receiptId) {
        long id = numericId(receiptId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT p.product_code, p.product_name, p.unit_name,
                            b.batch_code, b.expiry_date, gri.quantity, gri.unit_cost,
                            wl.location_code, wl.location_name
                     FROM goods_receipt_items gri
                     JOIN inventory_batches b ON b.batch_id = gri.batch_id
                     JOIN products p ON p.product_id = b.product_id
                     JOIN warehouse_locations wl ON wl.location_id = b.location_id
                     WHERE gri.receipt_id = ?
                     ORDER BY p.product_code, b.expiry_date, b.batch_code
                     """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ReceiptDetail> details = new ArrayList<>();
                while (resultSet.next()) {
                    java.sql.Date expiry = resultSet.getDate("expiry_date");
                    details.add(new ReceiptDetail(
                            resultSet.getString("product_code"),
                            resultSet.getString("product_name"),
                            resultSet.getString("unit_name"),
                            resultSet.getString("batch_code"),
                            expiry == null ? null : expiry.toLocalDate(),
                            resultSet.getBigDecimal("quantity"),
                            resultSet.getBigDecimal("unit_cost"),
                            resultSet.getString("location_code"),
                            resultSet.getString("location_name")));
                }
                return List.copyOf(details);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load goods receipt details.", exception);
        }
    }

    @Override
    public List<StocktakeDetail> findStocktakeDetails(UUID stocktakeId) {
        long id = numericId(stocktakeId);
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT p.product_code, p.product_name, p.unit_name,
                            b.batch_code, b.expiry_date,
                            sti.expected_quantity, sti.counted_quantity, sti.difference_quantity,
                            wl.location_code, wl.location_name
                     FROM stocktake_items sti
                     JOIN inventory_batches b ON b.batch_id = sti.batch_id
                     JOIN products p ON p.product_id = b.product_id
                     JOIN warehouse_locations wl ON wl.location_id = b.location_id
                     WHERE sti.stocktake_id = ?
                     ORDER BY p.product_code, b.expiry_date, b.batch_code
                     """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<StocktakeDetail> details = new ArrayList<>();
                while (resultSet.next()) {
                    java.sql.Date expiry = resultSet.getDate("expiry_date");
                    details.add(new StocktakeDetail(
                            resultSet.getString("product_code"),
                            resultSet.getString("product_name"),
                            resultSet.getString("unit_name"),
                            resultSet.getString("batch_code"),
                            expiry == null ? null : expiry.toLocalDate(),
                            resultSet.getBigDecimal("expected_quantity"),
                            resultSet.getBigDecimal("counted_quantity"),
                            resultSet.getBigDecimal("difference_quantity"),
                            resultSet.getString("location_code"),
                            resultSet.getString("location_name")));
                }
                return List.copyOf(details);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load stocktake details.", exception);
        }
    }

    @Override
    public ModuleRow receiveGoods(long authenticatedUserId, ReceiptRequest request) {
        validateReceiptRequest(authenticatedUserId, request);
        return transaction(connection -> {
            requireActiveRecord(connection,
                    "SELECT is_active FROM suppliers WHERE supplier_id = ?", request.supplierId(),
                    "Nhà cung cấp đã chọn không còn hoạt động.");
            for (ReceiptLine line : request.lines()) {
                requireActiveRecord(connection,
                        "SELECT is_active FROM products WHERE product_id = ?", line.productId(),
                        "Sản phẩm đã chọn không còn hoạt động.");
                requireActiveRecord(connection,
                        "SELECT is_active FROM warehouse_locations WHERE location_id = ?", line.locationId(),
                        "Vị trí kho đã chọn không còn hoạt động.");
            }
            String receiptCode = "PN-" + LocalDate.now().toString().replace("-", "")
                    + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            long receiptId = insert(connection, """
                    INSERT INTO goods_receipts
                        (receipt_code, supplier_id, received_by, received_at, status, note)
                    VALUES (?, ?, ?, ?, 'RECEIVED', ?)
                    """, statement -> {
                statement.setString(1, receiptCode);
                statement.setLong(2, request.supplierId());
                statement.setLong(3, authenticatedUserId);
                statement.setTimestamp(4, Timestamp.valueOf(request.receivedDate().atStartOfDay()));
                setNullableString(statement, 5, request.note());
            });

            for (ReceiptLine line : request.lines()) {
                long batchId = addReceivedQuantity(connection, line);
                insert(connection, """
                        INSERT INTO goods_receipt_items (receipt_id, batch_id, quantity, unit_cost)
                        VALUES (?, ?, ?, ?)
                        """, statement -> {
                    statement.setLong(1, receiptId);
                    statement.setLong(2, batchId);
                    statement.setBigDecimal(3, line.quantity());
                    statement.setBigDecimal(4, line.unitCost());
                });
                insertInventoryMovement(connection, batchId, "RECEIPT", line.quantity(),
                        "GOODS_RECEIPT", receiptId, request.receivedDate(), authenticatedUserId,
                        "Nhập hàng " + receiptCode);
            }

            String supplierName = resolveName(connection,
                    "SELECT supplier_name FROM suppliers WHERE supplier_id = ?", request.supplierId());
            return row(receiptId, List.of(receiptCode, supplierName,
                    request.receivedDate().toString(), request.lines().size() + " mặt hàng", "Đã nhận"));
        });
    }

    @Override
    public ModuleRow completeStocktake(long authenticatedUserId, StocktakeRequest request) {
        validateStocktakeRequest(authenticatedUserId, request);
        return transaction(connection -> {
            requireActiveRecord(connection,
                    "SELECT is_active FROM warehouse_locations WHERE location_id = ?", request.locationId(),
                    "Vị trí kho đã chọn không còn hoạt động.");
            String locationName = resolveName(connection,
                    "SELECT location_name FROM warehouse_locations WHERE location_id = ?", request.locationId());
            Map<Long, BigDecimal> currentQuantities = new java.util.LinkedHashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT batch_id, quantity_on_hand
                    FROM inventory_batches
                    WHERE location_id = ?
                    ORDER BY batch_id
                    FOR UPDATE
                    """)) {
                statement.setLong(1, request.locationId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        currentQuantities.put(resultSet.getLong("batch_id"),
                                resultSet.getBigDecimal("quantity_on_hand"));
                    }
                }
            }
            Set<Long> submittedBatchIds = new HashSet<>();
            for (StocktakeCount count : request.counts()) {
                if (!submittedBatchIds.add(count.batchId())) {
                    throw new IllegalArgumentException("Không được kiểm kê trùng một lô hàng.");
                }
            }
            if (currentQuantities.isEmpty() || !submittedBatchIds.equals(currentQuantities.keySet())) {
                throw new IllegalArgumentException(
                        "Danh sách lô tại vị trí đã thay đổi hoặc đang trống. Tải lại danh sách trước khi kiểm kê.");
            }

            long stocktakeId = insert(connection, """
                    INSERT INTO stocktakes (location_id, stocktake_date, status, created_by, note)
                    VALUES (?, ?, 'DRAFT', ?, ?)
                    """, statement -> {
                statement.setLong(1, request.locationId());
                statement.setDate(2, java.sql.Date.valueOf(request.stocktakeDate()));
                statement.setLong(3, authenticatedUserId);
                setNullableString(statement, 4, request.note());
            });
            BigDecimal totalVariance = BigDecimal.ZERO;
            for (StocktakeCount count : request.counts()) {
                BigDecimal expected = currentQuantities.get(count.batchId());
                BigDecimal difference = count.countedQuantity().subtract(expected);
                if (!isValidQuantity(difference)) {
                    throw new IllegalArgumentException(
                            "Chênh lệch vượt quá giới hạn lưu trữ; vui lòng kiểm kê lô này riêng.");
                }
                totalVariance = totalVariance.add(difference);
                insert(connection, """
                        INSERT INTO stocktake_items
                            (stocktake_id, batch_id, expected_quantity, counted_quantity, difference_quantity)
                        VALUES (?, ?, ?, ?, ?)
                        """, statement -> {
                    statement.setLong(1, stocktakeId);
                    statement.setLong(2, count.batchId());
                    statement.setBigDecimal(3, expected);
                    statement.setBigDecimal(4, count.countedQuantity());
                    statement.setBigDecimal(5, difference);
                });
                if (difference.signum() != 0) {
                    execute(connection, """
                            UPDATE inventory_batches SET quantity_on_hand = ?
                            WHERE batch_id = ? AND location_id = ?
                            """, statement -> {
                        statement.setBigDecimal(1, count.countedQuantity());
                        statement.setLong(2, count.batchId());
                        statement.setLong(3, request.locationId());
                    });
                    insertInventoryMovement(connection, count.batchId(), "ADJUSTMENT", difference,
                            "STOCKTAKE", stocktakeId, request.stocktakeDate(), authenticatedUserId,
                            "Điều chỉnh từ phiên KK-" + stocktakeId);
                }
            }
            execute(connection, "UPDATE stocktakes SET status = 'COMPLETED' WHERE stocktake_id = ?",
                    statement -> statement.setLong(1, stocktakeId));
            String variance = totalVariance.signum() > 0 ? "+" + formatQuantity(totalVariance)
                    : formatQuantity(totalVariance);
            return row(stocktakeId, List.of("KK-" + stocktakeId, locationName,
                    request.stocktakeDate().toString(), variance, "COMPLETED"));
        });
    }

    @Override
    public void recordInventoryLoss(
            long authenticatedUserId, long batchId, LossType lossType, BigDecimal quantity, String note) {
        if (note != null && note.length() > 255) {
            throw new IllegalArgumentException("Ghi chú điều chỉnh không được vượt quá 255 ký tự.");
        }
        if (authenticatedUserId < 1 || batchId < 1 || lossType == null
                || !isValidQuantity(quantity) || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Chọn lô hợp lệ và nhập số lượng hủy lớn hơn 0.");
        }
        transaction(connection -> {
            BigDecimal available;
            LocalDate expiryDate;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT quantity_on_hand, expiry_date
                    FROM inventory_batches
                    WHERE batch_id = ?
                    FOR UPDATE
                    """)) {
                statement.setLong(1, batchId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalArgumentException("Không tìm thấy lô hàng đã chọn.");
                    }
                    available = resultSet.getBigDecimal("quantity_on_hand");
                    java.sql.Date expiry = resultSet.getDate("expiry_date");
                    expiryDate = expiry == null ? null : expiry.toLocalDate();
                }
            }
            if (quantity.compareTo(available) > 0) {
                throw new IllegalArgumentException("Số lượng hủy vượt quá tồn kho hiện tại.");
            }
            if (lossType == LossType.EXPIRED
                    && (expiryDate == null || expiryDate.isAfter(LocalDate.now()))) {
                throw new IllegalArgumentException("Chỉ ghi nhận hết hạn cho lô đã đến hạn sử dụng.");
            }
            execute(connection,
                    "UPDATE inventory_batches SET quantity_on_hand = quantity_on_hand - ? WHERE batch_id = ?",
                    statement -> {
                        statement.setBigDecimal(1, quantity);
                        statement.setLong(2, batchId);
                    });
            insertInventoryMovement(connection, batchId, lossType.name(), quantity.negate(),
                    "MANUAL", null, LocalDate.now(), authenticatedUserId, note);
            return null;
        });
    }

    @Override
    public void replaceAll(String moduleId, List<List<String>> values) {
        requireSalesAdministrationAccess(moduleId);
        throw new UnsupportedOperationException("Bulk replacement is disabled to protect existing business data.");
    }

    @Override
    public ModuleRow create(String moduleId, List<String> values) {
        requireSalesAdministrationAccess(moduleId);
        return switch (moduleId) {
            case "UC-01" -> createEmployeeAccount(values);
            case "UC-02" -> createAvailability(values);
            case "UC-03" -> createStoreSchedule(values);
            case "UC-04" -> createAttendance(values);
            case "UC-07" -> createCategory(values);
            case "UC-08" -> createProduct(values);
            case "UC-09" -> createSupplier(values);
            case "UC-13" -> createLocation(values);
            case "UC-16" -> createCustomer(values);
            case "UC-18" -> createPromotion(values);
            default -> throw readOnly(moduleId);
        };
    }

    private ModuleRow createEmployeeAccount(List<String> values) {
        requireSize("UC-01", values, 7);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã nhân viên, họ tên và tên đăng nhập là bắt buộc.");
        }
        if (values.get(6).isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (values.get(6).getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu không được dài quá 72 byte UTF-8.");
        }
        if (!"STAFF".equals(normalizeRoleCode(values.get(5)))) {
            throw new IllegalArgumentException("Mục tạo tài khoản nhân viên chỉ hỗ trợ vai trò STAFF.");
        }
        String passwordHash = PasswordHasher.hash(values.get(6));
        long employeeId = transaction(connection -> {
            long roleId = resolveId(connection, "SELECT role_id FROM roles WHERE role_code = ?", "STAFF");
            long userId = insert(connection, """
                    INSERT INTO users (role_id, username, password_hash, is_active)
                    VALUES (?, ?, ?, 1)
                    """, statement -> {
                statement.setLong(1, roleId);
                statement.setString(2, values.get(4).trim());
                statement.setString(3, passwordHash);
            });
            return insert(connection, """
                    INSERT INTO employees
                        (user_id, employee_code, full_name, phone, email, hire_date, employment_status)
                    VALUES (?, ?, ?, ?, ?, CURRENT_DATE, 'ACTIVE')
                    """, statement -> {
                statement.setLong(1, userId);
                statement.setString(2, values.get(0).trim());
                statement.setString(3, values.get(1).trim());
                setNullableString(statement, 4, values.get(2));
                setNullableString(statement, 5, values.get(3));
            });
        });
        return row(employeeId, List.of(
                values.get(0).trim(), values.get(1).trim(), values.get(2).trim(), values.get(3).trim(),
                values.get(4).trim(), "Nhân viên", "Đang hoạt động"));
    }

    private ModuleRow updateEmployeeAccount(long employeeId, List<String> values) {
        requireSize("UC-01", values, 7);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã nhân viên, họ tên và tên đăng nhập là bắt buộc.");
        }
        String roleCode = normalizeRoleCode(values.get(5));
        final String username = values.get(4).trim();
        final long capturedEmployeeId = employeeId;
        final String employeeCode = values.get(0).trim();
        final String fullName = values.get(1).trim();
        final String phone = values.get(2);
        final String email = values.get(3);
        transaction(connection -> {
            final long roleId = resolveId(connection, "SELECT role_id FROM roles WHERE role_code = ?", roleCode);
            Long userId = null;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT user_id FROM employees WHERE employee_id = ?")) {
                ps.setLong(1, capturedEmployeeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long raw = rs.getLong(1);
                        if (!rs.wasNull()) userId = raw;
                    }
                }
            }
            final Long linkedUserId = userId;
            if (linkedUserId != null) {
                execute(connection, "UPDATE users SET role_id = ?, username = ? WHERE user_id = ?", st -> {
                    st.setLong(1, roleId);
                    st.setString(2, username);
                    st.setLong(3, linkedUserId);
                });
                if (!values.get(6).isBlank()) {
                    final String newHash = PasswordHasher.hash(values.get(6));
                    execute(connection, "UPDATE users SET password_hash = ? WHERE user_id = ?", st -> {
                        st.setString(1, newHash);
                        st.setLong(2, linkedUserId);
                    });
                }
            }
            execute(connection, """
                    UPDATE employees SET employee_code = ?, full_name = ?, phone = ?, email = ?
                    WHERE employee_id = ?
                    """, st -> {
                st.setString(1, employeeCode);
                st.setString(2, fullName);
                setNullableString(st, 3, phone);
                setNullableString(st, 4, email);
                st.setLong(5, capturedEmployeeId);
            });
            return null;
        });
        return row(employeeId, List.of(
                values.get(0).trim(), values.get(1).trim(), values.get(2).trim(), values.get(3).trim(),
                values.get(4).trim(), "ADMIN".equals(roleCode) ? "Quản trị viên" : "Nhân viên", "Đang hoạt động"));
    }

    private static String normalizeRoleCode(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) return "STAFF";
        String upper = rawRole.trim().toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(upper) || "QUẢN TRỊ VIÊN".equals(upper)) return "ADMIN";
        if ("STAFF".equals(upper) || "NHÂN VIÊN".equals(upper)) return "STAFF";
        throw new IllegalArgumentException("Vai trò chỉ được chọn Quản trị viên (ADMIN) hoặc Nhân viên (STAFF).");
    }

    @Override
    public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
        requireSalesAdministrationAccess(moduleId);
        long id = numericId(rowId);
        return switch (moduleId) {
            case "UC-01" -> updateEmployeeAccount(id, values);
            case "UC-03" -> updateStoreSchedule(id, values);
            case "UC-04" -> updateAttendance(id, values);
            case "UC-02" -> updateAvailability(id, values);
            case "UC-07" -> updateCategory(id, values);
            case "UC-08" -> updateProduct(id, values);
            case "UC-09" -> updateSupplier(id, values);
            case "UC-13" -> updateLocation(id, values);
            case "UC-16" -> updateCustomer(id, values);
            case "UC-18" -> updatePromotion(id, values);
            default -> throw readOnly(moduleId);
        };
    }

    @Override
    public void delete(String moduleId, UUID rowId) {
        requireSalesAdministrationAccess(moduleId);
        long id = numericId(rowId);
        if ("UC-08".equals(moduleId)) {
            transaction(connection -> {
                execute(connection, "DELETE FROM supplier_products WHERE product_id = ?",
                        statement -> statement.setLong(1, id));
                execute(connection, "DELETE FROM products WHERE product_id = ?",
                        statement -> statement.setLong(1, id));
                return null;
            });
            return;
        }
        if ("UC-09".equals(moduleId)) {
            transaction(connection -> {
                execute(connection, "DELETE FROM supplier_products WHERE supplier_id = ?",
                        statement -> statement.setLong(1, id));
                execute(connection, "DELETE FROM suppliers WHERE supplier_id = ?",
                        statement -> statement.setLong(1, id));
                return null;
            });
            return;
        }
        if ("UC-13".equals(moduleId)) {
            execute("""
                    UPDATE warehouse_locations SET is_active = 0
                    WHERE location_id = ?
                    """, statement -> statement.setLong(1, id));
            return;
        }
        if ("UC-16".equals(moduleId)) {
            execute("UPDATE customers SET is_active = 0 WHERE customer_id = ?",
                    statement -> statement.setLong(1, id));
            return;
        }
        String sql = switch (moduleId) {
            case "UC-02" -> "DELETE FROM employee_availabilities WHERE availability_id = ?";
            case "UC-03" -> "DELETE FROM schedule_assignments WHERE assignment_id = ?";
            case "UC-04" -> "DELETE FROM attendance_records WHERE attendance_id = ?";
            case "UC-07" -> "DELETE FROM product_categories WHERE category_id = ?";
            case "UC-18" -> "UPDATE promotions SET is_active = 0 WHERE promotion_id = ?";
            default -> throw readOnly(moduleId);
        };
        executeOne(sql, id);
    }

    private <T> List<T> queryOptions(String sql, RowMapper<T> mapper) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<T> options = new ArrayList<>();
            while (resultSet.next()) {
                options.add(mapper.read(resultSet));
            }
            return List.copyOf(options);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load inventory workflow options.", exception);
        }
    }

    private void validateReceiptRequest(long authenticatedUserId, ReceiptRequest request) {
        if (authenticatedUserId < 1 || request == null || request.supplierId() < 1
                || request.receivedDate() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("Nhà cung cấp, ngày nhập và ít nhất một dòng hàng là bắt buộc.");
        }
        if (request.receivedDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày nhập không được nằm trong tương lai.");
        }
        if (request.note() != null && request.note().length() > 255) {
            throw new IllegalArgumentException("Ghi chú phiếu nhập không được vượt quá 255 ký tự.");
        }
        Set<String> batchKeys = new HashSet<>();
        for (ReceiptLine line : request.lines()) {
            if (line == null || line.productId() < 1 || line.locationId() < 1
                    || line.batchCode() == null || line.batchCode().isBlank()
                    || line.batchCode().trim().length() > 50
                    || !isValidQuantity(line.quantity()) || line.quantity().signum() <= 0
                    || !isValidMoney(line.unitCost()) || line.unitCost().signum() < 0
                    || (line.expiryDate() != null && line.expiryDate().isBefore(request.receivedDate()))) {
                throw new IllegalArgumentException(
                        "Mỗi dòng nhập cần sản phẩm, vị trí, mã lô, số lượng hợp lệ và hạn dùng không trước ngày nhập.");
            }
            String key = line.productId() + ":" + line.locationId() + ":"
                    + line.batchCode().trim().toLowerCase(Locale.ROOT);
            if (!batchKeys.add(key)) {
                throw new IllegalArgumentException("Không được nhập trùng sản phẩm, vị trí và mã lô.");
            }
        }
    }

    private void validateStocktakeRequest(long authenticatedUserId, StocktakeRequest request) {
        if (authenticatedUserId < 1 || request == null || request.locationId() < 1
                || request.stocktakeDate() == null || request.counts().isEmpty()) {
            throw new IllegalArgumentException("Vị trí, ngày kiểm kê và danh sách lô là bắt buộc.");
        }
        if (request.stocktakeDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày kiểm kê không được nằm trong tương lai.");
        }
        if (request.note() != null && request.note().length() > 255) {
            throw new IllegalArgumentException("Ghi chú kiểm kê không được vượt quá 255 ký tự.");
        }
        for (StocktakeCount count : request.counts()) {
            if (count == null || count.batchId() < 1
                    || !isValidQuantity(count.countedQuantity())
                    || count.countedQuantity().signum() < 0) {
                throw new IllegalArgumentException("Số lượng kiểm kê phải là số không âm, tối đa 3 chữ số thập phân.");
            }
        }
    }

    private long addReceivedQuantity(Connection connection, ReceiptLine line) throws SQLException {
        long existingBatchId = 0;
        BigDecimal existingQuantity = null;
        LocalDate storedExpiry = null;
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT batch_id, expiry_date, quantity_on_hand
                FROM inventory_batches
                WHERE product_id = ? AND location_id = ? AND batch_code = ?
                FOR UPDATE
                """)) {
            statement.setLong(1, line.productId());
            statement.setLong(2, line.locationId());
            statement.setString(3, line.batchCode().trim());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    java.sql.Date existingExpiry = resultSet.getDate("expiry_date");
                    storedExpiry = existingExpiry == null ? null : existingExpiry.toLocalDate();
                    existingBatchId = resultSet.getLong("batch_id");
                    existingQuantity = resultSet.getBigDecimal("quantity_on_hand");
                }
            }
        }
        if (existingBatchId > 0) {
            long batchId = existingBatchId;
            if (!java.util.Objects.equals(storedExpiry, line.expiryDate())) {
                throw new IllegalArgumentException(
                        "Mã lô đã tồn tại tại vị trí này nhưng có hạn sử dụng khác.");
            }
            if (!isValidQuantity(existingQuantity.add(line.quantity()))) {
                throw new IllegalArgumentException("Tổng tồn lô vượt quá giới hạn lưu trữ.");
            }
            execute(connection, """
                    UPDATE inventory_batches
                    SET quantity_on_hand = quantity_on_hand + ?
                    WHERE batch_id = ?
                    """, update -> {
                update.setBigDecimal(1, line.quantity());
                update.setLong(2, batchId);
            });
            return batchId;
        }
        return insert(connection, """
                INSERT INTO inventory_batches
                    (product_id, location_id, batch_code, expiry_date, quantity_on_hand)
                VALUES (?, ?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, line.productId());
            statement.setLong(2, line.locationId());
            statement.setString(3, line.batchCode().trim());
            if (line.expiryDate() == null) {
                statement.setNull(4, java.sql.Types.DATE);
            } else {
                statement.setDate(4, java.sql.Date.valueOf(line.expiryDate()));
            }
            statement.setBigDecimal(5, line.quantity());
        });
    }

    private void insertInventoryMovement(
            Connection connection, long batchId, String movementType, BigDecimal quantity,
            String sourceType, Long sourceId, LocalDate movedOn, long userId, String note) throws SQLException {
        insert(connection, """
                INSERT INTO inventory_movements
                    (batch_id, movement_type, quantity_delta, source_type, source_id,
                     moved_at, created_by, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, batchId);
            statement.setString(2, movementType);
            statement.setBigDecimal(3, quantity);
            statement.setString(4, sourceType);
            if (sourceId == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, sourceId);
            }
            statement.setTimestamp(6, Timestamp.valueOf(movedOn.atStartOfDay()));
            statement.setLong(7, userId);
            setNullableString(statement, 8, note);
        });
    }

    private String resolveName(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Không tìm thấy dữ liệu liên quan đã chọn.");
                }
                return resultSet.getString(1);
            }
        }
    }

    private void requireActiveRecord(Connection connection, String sql, long id, String message)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !resultSet.getBoolean(1)) {
                    throw new IllegalArgumentException(message);
                }
            }
        }
    }

    private static boolean isValidQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return false;
        }
        BigDecimal normalized = quantity.stripTrailingZeros();
        return normalized.scale() <= 3 && normalized.precision() - normalized.scale() <= 9;
    }

    private static boolean isValidMoney(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        return normalized.scale() <= 2 && normalized.precision() - normalized.scale() <= 10;
    }

    private static String formatQuantity(BigDecimal quantity) {
        return quantity.setScale(3, java.math.RoundingMode.UNNECESSARY)
                .stripTrailingZeros().toPlainString();
    }

    private ModuleRow createAvailability(List<String> values) {
        requireSize("UC-02", values, 5);
        long employeeId = resolveId("SELECT employee_id FROM employees WHERE full_name = ? ORDER BY employee_id",
                values.get(0).trim());
        LocalDate date = parseIsoDate(values.get(1), "Ngày khả dụng");
        java.time.LocalTime start = parseIsoTime(values.get(2), "Giờ bắt đầu");
        java.time.LocalTime end = parseIsoTime(values.get(3), "Giờ kết thúc");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải sớm hơn giờ kết thúc.");
        }
        checkAvailabilityOverlap(employeeId, date, start, end, -1L);
        long id = insert("""
                INSERT INTO employee_availabilities
                    (employee_id, available_date, start_time, end_time, note)
                VALUES (?, ?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, employeeId);
            statement.setString(2, date.toString());
            statement.setString(3, start.toString());
            statement.setString(4, end.toString());
            setNullableString(statement, 5, values.get(4));
        });
        return row(id, List.of(values.get(0).trim(), date.toString(), start.toString(), end.toString(), values.get(4).trim()));
    }

    private ModuleRow updateAvailability(long id, List<String> values) {
        requireSize("UC-02", values, 5);
        long employeeId = resolveId("SELECT employee_id FROM employees WHERE full_name = ? ORDER BY employee_id",
                values.get(0).trim());
        LocalDate date = parseIsoDate(values.get(1), "Ngày khả dụng");
        java.time.LocalTime start = parseIsoTime(values.get(2), "Giờ bắt đầu");
        java.time.LocalTime end = parseIsoTime(values.get(3), "Giờ kết thúc");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải sớm hơn giờ kết thúc.");
        }
        checkAvailabilityOverlap(employeeId, date, start, end, id);
        execute("""
                UPDATE employee_availabilities
                SET employee_id = ?, available_date = ?, start_time = ?, end_time = ?, note = ?
                WHERE availability_id = ?
                """, statement -> {
            statement.setLong(1, employeeId);
            statement.setString(2, date.toString());
            statement.setString(3, start.toString());
            statement.setString(4, end.toString());
            setNullableString(statement, 5, values.get(4));
            statement.setLong(6, id);
        });
        return row(id, List.of(values.get(0).trim(), date.toString(), start.toString(), end.toString(), values.get(4).trim()));
    }

    private void checkAvailabilityOverlap(long employeeId, LocalDate date,
                                          java.time.LocalTime start, java.time.LocalTime end, long excludeId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT COUNT(*) FROM employee_availabilities
                     WHERE employee_id = ? AND available_date = ?
                       AND availability_id <> ?
                       AND start_time < ? AND end_time > ?
                     """)) {
            ps.setLong(1, employeeId);
            ps.setString(2, date.toString());
            ps.setLong(3, excludeId);
            ps.setString(4, end.toString());
            ps.setString(5, start.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new IllegalArgumentException("Nhân viên đã đăng ký khung giờ rảnh bị trùng vào ngày " + date + ".");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể kiểm tra trùng lịch khả dụng.", e);
        }
    }

    // --- UC-03: Quản lý lịch làm cửa hàng (Xếp lịch khả dụng + Xử lý trùng lịch & thiếu ca) ---
    private ModuleRow createStoreSchedule(List<String> values) {
        requireSize("UC-03", values, 5);
        LocalDate weekStart = parseIsoDate(values.get(0), "Tuần bắt đầu");
        LocalDate workDate = parseIsoDate(values.get(1), "Ngày làm");
        String shiftName = values.get(2).trim();
        String employeeName = values.get(3).trim();
        String note = values.get(4).trim();

        long employeeId = resolveId("SELECT employee_id FROM employees WHERE full_name = ? ORDER BY employee_id", employeeName);
        long shiftId = resolveId("SELECT shift_id FROM shifts WHERE shift_name = ? ORDER BY shift_id", shiftName);

        String finalNote = validateScheduleAndAvailability(employeeId, employeeName, shiftId, workDate, note, -1L);
        long assignmentId = transaction(connection -> {
            long scheduleId = ensureWorkSchedule(connection, weekStart);
            return insert(connection, """
                    INSERT INTO schedule_assignments (schedule_id, employee_id, shift_id, work_date, status, note)
                    VALUES (?, ?, ?, ?, 'PLANNED', ?)
                    """, st -> {
                st.setLong(1, scheduleId);
                st.setLong(2, employeeId);
                st.setLong(3, shiftId);
                st.setString(4, workDate.toString());
                setNullableString(st, 5, finalNote);
            });
        });
        return row(assignmentId, List.of(workDate.toString(), shiftName, employeeName, "PLANNED", finalNote));
    }

    private ModuleRow updateStoreSchedule(long assignmentId, List<String> values) {
        requireSize("UC-03", values, 5);
        LocalDate weekStart = parseIsoDate(values.get(0), "Tuần bắt đầu");
        LocalDate workDate = parseIsoDate(values.get(1), "Ngày làm");
        String shiftName = values.get(2).trim();
        String employeeName = values.get(3).trim();
        String note = values.get(4).trim();

        long employeeId = resolveId("SELECT employee_id FROM employees WHERE full_name = ? ORDER BY employee_id", employeeName);
        long shiftId = resolveId("SELECT shift_id FROM shifts WHERE shift_name = ? ORDER BY shift_id", shiftName);

        String finalNote = validateScheduleAndAvailability(employeeId, employeeName, shiftId, workDate, note, assignmentId);
        transaction(connection -> {
            long scheduleId = ensureWorkSchedule(connection, weekStart);
            execute(connection, """
                    UPDATE schedule_assignments
                    SET schedule_id = ?, employee_id = ?, shift_id = ?, work_date = ?, note = ?
                    WHERE assignment_id = ?
                    """, st -> {
                st.setLong(1, scheduleId);
                st.setLong(2, employeeId);
                st.setLong(3, shiftId);
                st.setString(4, workDate.toString());
                setNullableString(st, 5, finalNote);
                st.setLong(6, assignmentId);
            });
            return null;
        });
        return row(assignmentId, List.of(workDate.toString(), shiftName, employeeName, "PLANNED", finalNote));
    }

    private String validateScheduleAndAvailability(long employeeId, String employeeName,
                                                   long shiftId, LocalDate workDate, String note, long excludeAssignmentId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            // 1. Bắt lỗi trùng ca làm trong cùng ngày
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT s2.shift_name
                    FROM schedule_assignments sa
                    JOIN shifts s1 ON s1.shift_id = ?
                    JOIN shifts s2 ON sa.shift_id = s2.shift_id
                    WHERE sa.employee_id = ? AND sa.work_date = ?
                      AND sa.assignment_id <> ?
                      AND sa.status <> 'CANCELLED'
                      AND s1.start_time < s2.end_time AND s1.end_time > s2.start_time
                    """)) {
                ps.setLong(1, shiftId);
                ps.setLong(2, employeeId);
                ps.setString(3, workDate.toString());
                ps.setLong(4, excludeAssignmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new IllegalArgumentException("Trùng lịch: Nhân viên " + employeeName
                                + " đã được xếp '" + rs.getString(1) + "' trùng giờ vào ngày " + workDate + ".");
                    }
                }
            }
            // 2. Đối chiếu lịch khả dụng của nhân viên
            boolean hasAvailability = false;
            try (PreparedStatement ps = connection.prepareStatement("""
                    SELECT COUNT(*) FROM employee_availabilities ea
                    JOIN shifts s ON s.shift_id = ?
                    WHERE ea.employee_id = ? AND ea.available_date = ?
                      AND ea.start_time <= s.start_time AND ea.end_time >= s.end_time
                    """)) {
                ps.setLong(1, shiftId);
                ps.setLong(2, employeeId);
                ps.setString(3, workDate.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        hasAvailability = true;
                    }
                }
            }
            if (!hasAvailability && !note.contains("[Ngoài giờ đăng ký rảnh]")) {
                return note.isEmpty() ? "[Ngoài giờ đăng ký rảnh]" : note + " [Ngoài giờ đăng ký rảnh]";
            }
            return note;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể kiểm tra ràng buộc lịch làm.", e);
        }
    }

    private long ensureWorkSchedule(Connection connection, LocalDate weekStart) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT schedule_id FROM work_schedules WHERE week_start_date = ?")) {
            ps.setString(1, weekStart.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        long creatorId;
        try (PreparedStatement ps = connection.prepareStatement("SELECT user_id FROM users ORDER BY user_id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Chưa có tài khoản quản trị trong bảng users.");
            creatorId = rs.getLong(1);
        }
        return insert(connection,
                "INSERT INTO work_schedules (week_start_date, status, created_by) VALUES (?, 'PUBLISHED', ?)",
                st -> {
                    st.setString(1, weekStart.toString());
                    st.setLong(2, creatorId);
                });
    }

    // --- UC-04: Quản lý bảng chấm công ---
    private ModuleRow createAttendance(List<String> values) {
        requireSize("UC-04", values, 5);
        String employeeName = values.get(0).trim();
        String shiftName = values.get(1).trim();
        String checkIn = values.get(2).trim();
        String checkOut = values.get(3).trim();
        String note = values.get(4).trim();

        if (checkIn.isEmpty()) {
            throw new IllegalArgumentException("Giờ vào (YYYY-MM-DD HH:mm) là bắt buộc.");
        }
        long assignmentId = resolveLatestAssignmentId(employeeName, shiftName);
        long attendanceId = transaction(connection -> {
            long id = insert(connection, """
                    INSERT INTO attendance_records (assignment_id, check_in_at, check_out_at, note)
                    VALUES (?, ?, ?, ?)
                    """, st -> {
                st.setLong(1, assignmentId);
                st.setString(2, checkIn);
                setNullableString(st, 3, checkOut);
                setNullableString(st, 4, note);
            });
            if (!checkOut.isEmpty()) {
                execute(connection, "UPDATE schedule_assignments SET status = 'COMPLETED' WHERE assignment_id = ?",
                        st -> st.setLong(1, assignmentId));
            }
            return id;
        });
        return row(attendanceId, List.of(employeeName, shiftName, checkIn, checkOut, note));
    }

    private ModuleRow updateAttendance(long attendanceId, List<String> values) {
        requireSize("UC-04", values, 5);
        String employeeName = values.get(0).trim();
        String shiftName = values.get(1).trim();
        String checkIn = values.get(2).trim();
        String checkOut = values.get(3).trim();
        String note = values.get(4).trim();

        transaction(connection -> {
            execute(connection, """
                    UPDATE attendance_records SET check_in_at = ?, check_out_at = ?, note = ?
                    WHERE attendance_id = ?
                    """, st -> {
                st.setString(1, checkIn);
                setNullableString(st, 2, checkOut);
                setNullableString(st, 3, note);
                st.setLong(4, attendanceId);
            });
            if (!checkOut.isEmpty()) {
                execute(connection, """
                        UPDATE schedule_assignments sa
                        JOIN attendance_records ar ON ar.assignment_id = sa.assignment_id
                        SET sa.status = 'COMPLETED'
                        WHERE ar.attendance_id = ?
                        """, st -> st.setLong(1, attendanceId));
            }
            return null;
        });
        return row(attendanceId, List.of(employeeName, shiftName, checkIn, checkOut, note));
    }

    private long resolveLatestAssignmentId(String employeeName, String shiftName) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement("""
                     SELECT sa.assignment_id
                     FROM schedule_assignments sa
                     JOIN employees e ON e.employee_id = sa.employee_id
                     JOIN shifts s ON s.shift_id = sa.shift_id
                     WHERE e.full_name = ? AND s.shift_name = ? AND sa.status <> 'CANCELLED'
                     ORDER BY sa.work_date DESC, sa.assignment_id DESC LIMIT 1
                     """)) {
            ps.setString(1, employeeName);
            ps.setString(2, shiftName);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Không tìm thấy ca được phân phù hợp cho nhân viên " + employeeName + ".");
                }
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Không thể tra cứu ca phân công để chấm công.", e);
        }
    }

    private static LocalDate parseIsoDate(String raw, String label) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(label + " phải đúng định dạng YYYY-MM-DD (ví dụ: 2026-11-20).");
        }
    }

    private static java.time.LocalTime parseIsoTime(String raw, String label) {
        try {
            String trimmed = raw.trim();
            if (trimmed.length() == 5) trimmed += ":00";
            return java.time.LocalTime.parse(trimmed);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException(label + " phải đúng định dạng HH:mm (ví dụ: 08:00).");
        }
    }

    private ModuleRow createCategory(List<String> values) {
        requireSize("UC-07", values, 3);
        if (values.get(0).isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        long id = insert("""
                INSERT INTO product_categories (category_name, description, is_active)
                VALUES (?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0).trim());
            setNullableString(statement, 2, values.get(1));
            statement.setBoolean(3, isActive(values.get(2)));
        });
        return row(id, List.of(values.get(0).trim(), values.get(1).trim(),
                isActive(values.get(2)) ? "ACTIVE" : "INACTIVE"));
    }

    private ModuleRow updateCategory(long id, List<String> values) {
        requireSize("UC-07", values, 3);
        if (values.get(0).isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        execute("""
                UPDATE product_categories SET category_name = ?, description = ?, is_active = ?
                WHERE category_id = ?
                """, statement -> {
            statement.setString(1, values.get(0).trim());
            setNullableString(statement, 2, values.get(1));
            statement.setBoolean(3, isActive(values.get(2)));
            statement.setLong(4, id);
        });
        return row(id, List.of(values.get(0).trim(), values.get(1).trim(),
                isActive(values.get(2)) ? "ACTIVE" : "INACTIVE"));
    }
    private ModuleRow createProduct(List<String> values) {
        validateProductFields(values);
        java.math.BigDecimal unitPrice = parseUnitPrice(values.get(5));
        long categoryId = resolveId("SELECT category_id FROM product_categories WHERE category_name = ?",
                values.get(2).trim());
        long productId = transaction(connection -> {
            long id = insert(connection, """
                    INSERT INTO products
                        (category_id, product_code, barcode, product_name, dosage_form, unit_name,
                        selling_price, requires_prescription, is_active)
                    VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?)
                    """, statement -> {
                statement.setLong(1, categoryId);
                statement.setString(2, values.get(0).trim());
                statement.setString(3, values.get(1).trim());
                setNullableString(statement, 4, values.get(3));
                statement.setString(5, values.get(4).trim());
                statement.setBigDecimal(6, unitPrice);
                statement.setBoolean(7, affirmative(values.get(6)));
                statement.setBoolean(8, isActive(values.get(7)));
            });
            synchronizeProductSuppliers(connection, id, values.get(8));
            return id;
        });
        return row(productId, values);
    }

    private ModuleRow updateProduct(long id, List<String> values) {
        validateProductFields(values);
        java.math.BigDecimal unitPrice = parseUnitPrice(values.get(5));
        long categoryId = resolveId("SELECT category_id FROM product_categories WHERE category_name = ?",
                values.get(2).trim());
        transaction(connection -> {
            execute(connection, """
                    UPDATE products SET category_id = ?, product_code = ?, product_name = ?,
                           dosage_form = ?, unit_name = ?,selling_price = ?, requires_prescription = ?, is_active = ?
                    WHERE product_id = ?
                    """, statement -> {
                statement.setLong(1, categoryId);
                statement.setString(2, values.get(0).trim());
                statement.setString(3, values.get(1).trim());
                setNullableString(statement, 4, values.get(3));
                statement.setString(5, values.get(4).trim());
                statement.setBigDecimal(6, unitPrice);
                statement.setBoolean(7, affirmative(values.get(6)));
                statement.setBoolean(8, isActive(values.get(7)));
                statement.setLong(9, id);
            });
            synchronizeProductSuppliers(connection, id, values.get(8));
            return null;
        });
        return row(id, values);
    }

    private void validateProductFields(List<String> values) {
        requireSize("UC-08", values, 9);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(2).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm, tên sản phẩm, danh mục và đơn vị tính là bắt buộc.");
        }
        requireFieldLength(values.get(0).trim(), 30, "Mã sản phẩm");
        requireFieldLength(values.get(1).trim(), 180, "Tên sản phẩm");
        requireFieldLength(values.get(3), 80, "Dạng bào chế");
        requireFieldLength(values.get(4).trim(), 30, "Đơn vị tính");
    }

    static java.math.BigDecimal parseUnitPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            String cleaned = raw.replace("VNĐ", "").replace("VND", "").replace("đ", "").replace("₫", "")
                    .replaceAll("\\s+", "");
            if (cleaned.startsWith("-")) {
                throw new IllegalArgumentException("Giá bán không được âm.");
            }
            if (cleaned.matches("\\d+(?:[.,]\\d{1,2})?")) {
                cleaned = cleaned.replace(',', '.');
            } else if (cleaned.matches("\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?")) {
                cleaned = cleaned.replace(".", "").replace(',', '.');
            } else if (cleaned.matches("\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?")) {
                cleaned = cleaned.replace(",", "");
            } else {
                throw new NumberFormatException();
            }
            java.math.BigDecimal price = new java.math.BigDecimal(cleaned)
                    .setScale(2);
            if (price.compareTo(new java.math.BigDecimal("9999999999.99")) > 0) {
                throw new IllegalArgumentException("Giá bán vượt quá giới hạn cho phép.");
            }
            return price;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Giá bán phải là số hợp lệ (ví dụ: 25000).");
        }
    }
    private void synchronizeProductSuppliers(Connection connection, long productId, String supplierNames)
            throws SQLException {
        List<Long> desiredSupplierIds = new ArrayList<>();
        Set<String> normalizedNames = new LinkedHashSet<>();
        if (supplierNames != null && !supplierNames.isBlank()) {
            for (String part : supplierNames.split(",", -1)) {
                String name = part.trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Tên nhà cung cấp không được để trống giữa các dấu phẩy.");
                }
                if (!normalizedNames.add(name.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Tên nhà cung cấp bị trùng.");
                }
                desiredSupplierIds.add(resolveId(connection,
                        "SELECT supplier_id FROM suppliers WHERE supplier_name = ?", name));
            }
        }

        List<Long> existingSupplierIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT supplier_id FROM supplier_products WHERE product_id = ?")) {
            statement.setLong(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    existingSupplierIds.add(resultSet.getLong(1));
                }
            }
        }
        for (long existingSupplierId : existingSupplierIds) {
            if (!desiredSupplierIds.contains(existingSupplierId)) {
                execute(connection,
                        "DELETE FROM supplier_products WHERE supplier_id = ? AND product_id = ?",
                        statement -> {
                            statement.setLong(1, existingSupplierId);
                            statement.setLong(2, productId);
                        });
            }
        }
        for (long supplierId : desiredSupplierIds) {
            execute(connection, """
                    INSERT INTO supplier_products (supplier_id, product_id, purchase_price)
                    VALUES (?, ?, 0)
                    ON DUPLICATE KEY UPDATE purchase_price = supplier_products.purchase_price
                    """, statement -> {
                statement.setLong(1, supplierId);
                statement.setLong(2, productId);
            });
        }
    }

    private ModuleRow createSupplier(List<String> values) {
        requireSize("UC-09", values, 6);
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            throw new IllegalArgumentException("Mã nhà cung cấp và tên nhà cung cấp là bắt buộc.");
        }
        long id = insert("""
                INSERT INTO suppliers (supplier_code, supplier_name, phone, email, address, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0).trim());
            statement.setString(2, values.get(1).trim());
            setNullableString(statement, 3, values.get(2));
            setNullableString(statement, 4, values.get(3));
            setNullableString(statement, 5, values.get(4));
            statement.setBoolean(6, isActive(values.get(5)));
        });
        return row(id, values);
    }

    private ModuleRow updateSupplier(long id, List<String> values) {
        requireSize("UC-09", values, 6);
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            throw new IllegalArgumentException("Mã nhà cung cấp và tên nhà cung cấp là bắt buộc.");
        }
        execute("""
                UPDATE suppliers SET supplier_code = ?, supplier_name = ?, phone = ?, email = ?, address = ?, is_active = ?
                WHERE supplier_id = ?
                """, statement -> {
            statement.setString(1, values.get(0).trim());
            statement.setString(2, values.get(1).trim());
            setNullableString(statement, 3, values.get(2));
            setNullableString(statement, 4, values.get(3));
            setNullableString(statement, 5, values.get(4));
            statement.setBoolean(6, isActive(values.get(5)));
            statement.setLong(7, id);
        });
        return row(id, values);
    }

    private ModuleRow createLocation(List<String> values) {
        requireSize("UC-13", values, 4);
        validateLocation(values);
        long id = insert("""
                INSERT INTO warehouse_locations (location_code, location_name, description, is_active)
                VALUES (?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0).trim());
            statement.setString(2, values.get(1).trim());
            setNullableString(statement, 3, values.get(2));
            statement.setBoolean(4, isActive(values.get(3)));
        });
        return row(id, List.of(values.get(0).trim(), values.get(1).trim(), values.get(2).trim(),
                isActive(values.get(3)) ? "ACTIVE" : "INACTIVE"));
    }

    private ModuleRow updateLocation(long id, List<String> values) {
        requireSize("UC-13", values, 4);
        validateLocation(values);
        execute("""
                UPDATE warehouse_locations SET location_code = ?, location_name = ?,
                    description = ?, is_active = ? WHERE location_id = ?
                """, statement -> {
            statement.setString(1, values.get(0).trim());
            statement.setString(2, values.get(1).trim());
            setNullableString(statement, 3, values.get(2));
            statement.setBoolean(4, isActive(values.get(3)));
            statement.setLong(5, id);
        });
        return row(id, List.of(values.get(0).trim(), values.get(1).trim(), values.get(2).trim(),
                isActive(values.get(3)) ? "ACTIVE" : "INACTIVE"));
    }

    private void validateLocation(List<String> values) {
        if (values.get(0).isBlank() || values.get(1).isBlank()) {
            throw new IllegalArgumentException("Mã vị trí và tên vị trí kho không được để trống.");
        }
        if (values.get(0).trim().length() > 30 || values.get(1).trim().length() > 100
                || values.get(2).length() > 255) {
            throw new IllegalArgumentException("Mã, tên hoặc mô tả vị trí vượt quá độ dài cho phép.");
        }
        if (!"ACTIVE".equalsIgnoreCase(values.get(3))
                && !"INACTIVE".equalsIgnoreCase(values.get(3))) {
            throw new IllegalArgumentException("Trạng thái vị trí chỉ được là Đang sử dụng hoặc Ngừng sử dụng.");
        }
    }

    private ModuleRow createCustomer(List<String> values) {
        requireSize("UC-16", values, 9);
        validateCustomer(values);
        long initialPoints = parseInitialPoints(values.get(5));
        List<CustomerAllergy> allergies = parseCustomerAllergies(values.get(4));
        String status = normalizeCustomerStatus(values.get(6));
        return transaction(connection -> {
            String code = values.get(0).isBlank()
                    ? "KH" + Long.toUnsignedString(System.nanoTime(), 36).toUpperCase(Locale.ROOT)
                    : values.get(0).trim();
            long customerId = insert(connection, """
                    INSERT INTO customers
                        (customer_code, full_name, phone, email, date_of_birth, loyalty_points,
                         is_active, is_blocked, block_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, statement -> {
                statement.setString(1, code);
                statement.setString(2, values.get(1));
                statement.setString(3, blankToNull(values.get(2)));
                statement.setString(4, blankToNull(values.get(3)));
                setCustomerDate(statement, 5, values.get(8));
                statement.setLong(6, initialPoints);
                statement.setBoolean(7, customerIsActive(status));
                statement.setBoolean(8, customerIsBlocked(status));
                statement.setString(9, customerIsBlocked(status) ? blankToNull(values.get(7)) : null);
            });
            replaceAllergies(connection, customerId, allergies);
            return readUpdatedCustomer(connection, customerId, values);
        });
    }

    private ModuleRow updateCustomer(long id, List<String> values) {
        return updateCustomer(id, values, true);
    }

    @Override
    public ModuleRow updateCustomerProfile(UUID customerId, List<String> values) {
        return updateCustomer(numericId(customerId), values, false);
    }

    @Override
    public void deleteCustomerWithoutSales(UUID customerId) {
        requireAdministrator();
        long id = numericId(customerId);
        transaction(connection -> {
            try (PreparedStatement customer = connection.prepareStatement(
                    "SELECT customer_id FROM customers WHERE customer_id = ? FOR UPDATE")) {
                customer.setLong(1, id);
                try (ResultSet row = customer.executeQuery()) {
                    if (!row.next()) {
                        throw new IllegalStateException(Localization.text("Không tìm thấy hồ sơ khách hàng."));
                    }
                }
            }
            // Checkout also locks the customer row before creating or changing an order for this customer.
            try (PreparedStatement sales = connection.prepareStatement(
                    "SELECT 1 FROM sales WHERE customer_id = ? LIMIT 1")) {
                sales.setLong(1, id);
                try (ResultSet rows = sales.executeQuery()) {
                    if (rows.next()) {
                        throw new IllegalStateException(Localization.text(
                                "Khách hàng đã có đơn hàng. Hãy lưu trữ hồ sơ."));
                    }
                }
            }
            replaceAllergies(connection, id, List.of());
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM customers WHERE customer_id = ?")) {
                delete.setLong(1, id);
                if (delete.executeUpdate() != 1) {
                    throw new SQLException("Could not delete the locked customer.");
                }
            }
            return null;
        });
    }

    private ModuleRow updateCustomer(long id, List<String> values, boolean updateStatus) {
        requireSize("UC-16", values, 9);
        if (updateStatus) {
            validateCustomer(values);
        } else {
            validateCustomerDetails(values);
        }
        List<CustomerAllergy> allergies = parseCustomerAllergies(values.get(4));
        String sql = "UPDATE customers SET full_name = ?, phone = ?, email = ?, date_of_birth = ?"
                + (updateStatus ? ", customer_code = ?, is_active = ?, is_blocked = ?, block_reason = ?" : "")
                + " WHERE customer_id = ?";
        return transaction(connection -> {
            execute(connection, sql, statement -> {
                statement.setString(1, values.get(1));
                statement.setString(2, blankToNull(values.get(2)));
                statement.setString(3, blankToNull(values.get(3)));
                setCustomerDate(statement, 4, values.get(8));
                if (updateStatus) {
                    String status = normalizeCustomerStatus(values.get(6));
                    statement.setString(5, values.get(0).trim());
                    statement.setBoolean(6, customerIsActive(status));
                    statement.setBoolean(7, customerIsBlocked(status));
                    statement.setString(8, customerIsBlocked(status) ? blankToNull(values.get(7)) : null);
                }
                statement.setLong(updateStatus ? 9 : 5, id);
            });
            ModuleRow updated = readUpdatedCustomer(connection, id, values);
            replaceAllergies(connection, id, allergies);
            return updated;
        });
    }

    private ModuleRow readUpdatedCustomer(Connection connection, long id, List<String> values)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT customer_code, loyalty_points, is_active, is_blocked, COALESCE(block_reason, '') AS block_reason
                FROM customers WHERE customer_id = ?
                """)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Không tìm thấy hồ sơ khách hàng.");
                }
                boolean active = resultSet.getBoolean("is_active");
                boolean blocked = resultSet.getBoolean("is_blocked");
                String status = active ? (blocked ? "BLOCKED" : "ACTIVE")
                        : (blocked ? "ARCHIVED_BLOCKED" : "ARCHIVED");
                return row(id, List.of(
                        resultSet.getString("customer_code"), values.get(1), values.get(2),
                        values.get(3), values.get(4), resultSet.getString("loyalty_points"),
                        status, resultSet.getString("block_reason"), values.get(8)));
            }
        }
    }

    private void validateCustomer(List<String> values) {
        validateCustomerDetails(values);
        requireFieldLength(values.get(0).trim(), 20, "Mã khách hàng");
        requireFieldLength(values.get(7), 255, "Lý do hạn chế bán");
        String status = normalizeCustomerStatus(values.get(6));
        if (customerIsBlocked(status) && values.get(7).isBlank()) {
            throw new IllegalArgumentException("Cần nhập lý do khi hạn chế bán.");
        }
    }

    private void validateCustomerDetails(List<String> values) {
        if (values.get(1).isBlank()) {
            throw new IllegalArgumentException("Tên khách hàng là bắt buộc.");
        }
        requireFieldLength(values.get(1), 120, "Tên khách hàng");
        requireFieldLength(values.get(2), 20, "Số điện thoại");
        requireFieldLength(values.get(3), 120, "Email");
        String phone = values.get(2).replaceAll("[\\s().-]", "");
        String email = values.get(3).trim();
        if (!phone.isEmpty() && !phone.matches("\\+?[0-9]{7,15}")) {
            throw new IllegalArgumentException("Số điện thoại cần có từ 7 đến 15 chữ số.");
        }
        if (!email.isEmpty() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (phone.isEmpty() && email.isEmpty()) {
            throw new IllegalArgumentException("Cần nhập số điện thoại hoặc email của khách hàng.");
        }
        parseCustomerAllergies(values.get(4));
        if (!values.get(8).isBlank()) {
            LocalDate birthday;
            try {
                birthday = LocalDate.parse(values.get(8));
            } catch (java.time.format.DateTimeParseException exception) {
                throw new IllegalArgumentException("Ngày sinh phải theo định dạng YYYY-MM-DD.", exception);
            }
            if (birthday.getYear() < 1000 || birthday.isAfter(LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")))) {
                throw new IllegalArgumentException("Ngày sinh phải hợp lệ và không ở tương lai.");
            }
        }
    }

    private static void requireFieldLength(String value, int maximum, String label) {
        if (value.codePointCount(0, value.length()) > maximum) {
            throw new IllegalArgumentException(Localization.text(label) + " "
                    + Localization.text("vượt quá ") + maximum + Localization.text(" ký tự."));
        }
    }

    private static long parseInitialPoints(String value) {
        try {
            long points = Long.parseLong(value.trim());
            if (points >= 0 && points <= SaleOrderRequest.MAX_POINT_BALANCE) {
                return points;
            }
        } catch (NumberFormatException exception) {
            // Report the permitted range below.
        }
        throw new IllegalArgumentException(Localization.text("Số điểm phải là số nguyên từ 0 đến 4294967295."));
    }

    private String normalizeCustomerStatus(String status) {
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE", "BLOCKED", "ARCHIVED", "ARCHIVED_BLOCKED" -> status.trim().toUpperCase(Locale.ROOT);
            default -> throw new IllegalArgumentException("Trạng thái khách hàng không hợp lệ.");
        };
    }

    private boolean customerIsActive(String status) {
        return "ACTIVE".equals(status) || "BLOCKED".equals(status);
    }

    private boolean customerIsBlocked(String status) {
        return "BLOCKED".equals(status) || "ARCHIVED_BLOCKED".equals(status);
    }

    private void setCustomerDate(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(LocalDate.parse(value)));
        }
    }

    private List<CustomerAllergy> parseCustomerAllergies(String allergyValues) {
        List<CustomerAllergy> allergies = new ArrayList<>();
        Set<String> normalizedNames = new LinkedHashSet<>();
        if (allergyValues == null || allergyValues.isBlank()) {
            return allergies;
        }
        for (String line : allergyValues.split("\\R")) {
            String[] parts = line.split("\\|", 2);
            String name = parts[0].trim();
            if (name.isEmpty()) {
                continue;
            }
            if (!normalizedNames.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Tên dị ứng bị trùng.");
            }
            allergies.add(new CustomerAllergy(name, parts.length > 1 ? blankToNull(parts[1].trim()) : null));
        }
        return List.copyOf(allergies);
    }

    private void replaceAllergies(
            Connection connection, long customerId, List<CustomerAllergy> allergies) throws SQLException {
        execute(connection, "DELETE FROM customer_allergies WHERE customer_id = ?",
                statement -> statement.setLong(1, customerId));
        if (allergies.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customer_allergies (customer_id, allergen_name, note) VALUES (?, ?, ?)")) {
            for (CustomerAllergy allergy : allergies) {
                statement.setLong(1, customerId);
                statement.setString(2, allergy.name());
                statement.setString(3, allergy.note());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private List<ModuleRow> findPromotions() {
        List<ModuleRow> joinedRows = query("UC-18", """
                SELECT p.promotion_id, p.promotion_code, p.promotion_name, p.discount_percent,
                       DATE_FORMAT(p.starts_at, '%Y-%m-%d'),
                       DATE_FORMAT(p.points_redemption_starts_at, '%Y-%m-%d'),
                       DATE_FORMAT(p.points_earning_ends_at, '%Y-%m-%d'),
                       DATE_FORMAT(p.ends_at, '%Y-%m-%d'), p.redemption_vnd_per_point,
                       IF(p.is_active, 'ACTIVE', 'INACTIVE'),
                       CONCAT(t.minimum_invoice_total_vnd, ':', t.points_rate_percent)
                FROM promotions p
                LEFT JOIN promotion_point_tiers t ON t.promotion_id = p.promotion_id
                ORDER BY p.promotion_id, t.minimum_invoice_total_vnd
                """);
        Map<UUID, List<ModuleRow>> rowsByPromotion = new LinkedHashMap<>();
        for (ModuleRow joinedRow : joinedRows) {
            rowsByPromotion.computeIfAbsent(joinedRow.id(), ignored -> new ArrayList<>()).add(joinedRow);
        }
        return rowsByPromotion.values().stream().map(promotionRows -> {
            ModuleRow firstRow = promotionRows.getFirst();
            List<String> values = new ArrayList<>(firstRow.values().subList(0, 9));
            String tiers = promotionRows.stream()
                    .map(joinedRow -> joinedRow.values().get(9))
                    .filter(tier -> !tier.isBlank())
                    .collect(Collectors.joining(";"));
            values.add(8, tiers);
            return new ModuleRow(firstRow.id(), values);
        }).toList();
    }

    private ModuleRow createPromotion(List<String> values) {
        requireSize("UC-18", values, 10);
        long id = transaction(connection -> {
            long promotionId = insert(connection, """
                    INSERT INTO promotions
                        (promotion_code, promotion_name, discount_percent, starts_at,
                         points_redemption_starts_at, points_earning_ends_at, ends_at,
                         redemption_vnd_per_point, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, statement -> bindPromotion(statement, values));
            insertPromotionTiers(connection, promotionId, values.get(8));
            return promotionId;
        });
        return row(id, values);
    }

    private ModuleRow updatePromotion(long id, List<String> values) {
        requireSize("UC-18", values, 10);
        transaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE promotions SET promotion_code = ?, promotion_name = ?, discount_percent = ?,
                        starts_at = ?, points_redemption_starts_at = ?, points_earning_ends_at = ?,
                        ends_at = ?, redemption_vnd_per_point = ?, is_active = ?
                    WHERE promotion_id = ?
                    """)) {
                bindPromotion(statement, values);
                statement.setLong(10, id);
                if (statement.executeUpdate() != 1) {
                    throw new IllegalArgumentException("The selected program no longer exists.");
                }
            }
            execute(connection, "DELETE FROM promotion_point_tiers WHERE promotion_id = ?",
                    statement -> statement.setLong(1, id));
            insertPromotionTiers(connection, id, values.get(8));
            return null;
        });
        return row(id, values);
    }

    @Override
    public void deactivatePromotion(UUID promotionId, Optional<LocalDate> endOn) {
        requireAdministrator();
        if (endOn.isEmpty()) {
            delete("UC-18", promotionId);
            return;
        }
        long id = numericId(promotionId);
        var endOfDay = endOn.orElseThrow().atTime(23, 59, 59);
        execute("""
                UPDATE promotions SET is_active = 0,
                    points_earning_ends_at = CASE WHEN points_redemption_starts_at <= ?
                        THEN LEAST(points_earning_ends_at, ?) ELSE points_earning_ends_at END,
                    ends_at = CASE WHEN points_redemption_starts_at <= ?
                        THEN LEAST(ends_at, ?) ELSE ends_at END
                WHERE promotion_id = ?
                """, statement -> {
            statement.setObject(1, endOfDay);
            statement.setObject(2, endOfDay);
            statement.setObject(3, endOfDay);
            statement.setObject(4, endOfDay);
            statement.setLong(5, id);
        });
    }

    private static void bindPromotion(PreparedStatement statement, List<String> values) throws SQLException {
        statement.setString(1, values.get(0));
        statement.setString(2, values.get(1));
        statement.setBigDecimal(3, promotionDecimal(values.get(2)));
        statement.setObject(4, LocalDate.parse(values.get(3)).atStartOfDay());
        statement.setObject(5, LocalDate.parse(values.get(4)).atStartOfDay());
        statement.setObject(6, LocalDate.parse(values.get(5)).atTime(23, 59, 59));
        statement.setObject(7, LocalDate.parse(values.get(6)).atTime(23, 59, 59));
        statement.setBigDecimal(8, promotionDecimal(values.get(7)));
        statement.setBoolean(9, parseStatus(values.get(9)));
    }

    private static void insertPromotionTiers(Connection connection, long promotionId, String encodedTiers)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO promotion_point_tiers (promotion_id, minimum_invoice_total_vnd, points_rate_percent)
                VALUES (?, ?, ?)
                """)) {
            for (String tier : encodedTiers.split(";", -1)) {
                String[] parts = tier.split(":", -1);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Each points tier must contain threshold:rate.");
                }
                statement.setLong(1, promotionId);
                statement.setBigDecimal(2, promotionDecimal(parts[0]));
                statement.setBigDecimal(3, promotionDecimal(parts[1]));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static BigDecimal promotionDecimal(String value) {
        return new BigDecimal(value.trim().replace(',', '.'));
    }

    private List<ModuleRow> query(String moduleId, String sql) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ModuleRow> rows = new ArrayList<>();
            int columns = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                List<String> values = new ArrayList<>(columns - 1);
                for (int index = 2; index <= columns; index++) {
                    String value = resultSet.getString(index);
                    values.add(value == null ? "" : value);
                }
                rows.add(row(id, values));
            }
            return List.copyOf(rows);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not read " + moduleId + " from the provided MySQL schema.",
                    exception);
        }
    }

    private long resolveId(String sql, String name) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return resolveId(connection, sql, name);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not resolve the related record.", exception);
        }
    }

    private long resolveId(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("Related record not found: " + name);
                }
                long id = resultSet.getLong(1);
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Related record name is ambiguous: " + name);
                }
                return id;
            }
        }
    }

    private long insert(String sql, Binder binder) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return insert(connection, sql, binder);
        } catch (SQLException exception) {
            throw friendly(exception, "Could not create the business record.");
        }
    }

    private long insert(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(statement);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("MySQL did not return a generated key.");
                }
                return keys.getLong(1);
            }
        }
    }

    private void executeOne(String sql, long id) {
        execute(sql, statement -> statement.setLong(1, id));
    }

    private void execute(String sql, Binder binder) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            execute(connection, sql, binder);
        } catch (SQLException exception) {
            throw friendly(exception, "Could not update the business record.");
        }
    }

    private void execute(Connection connection, String sql, Binder binder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        }
    }

    private static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value.trim());
        }
    }

    private <T> T transaction(TransactionWork<T> work) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            throw friendly(exception, "Không thể lưu thay đổi vào cơ sở dữ liệu.");
        }
    }

    private static ModuleRow row(long id, List<String> values) {
        return new ModuleRow(new UUID(ROW_ID_PREFIX, id), values);
    }

    private static long numericId(UUID id) {
        if (id == null || id.getMostSignificantBits() != ROW_ID_PREFIX || id.getLeastSignificantBits() < 1) {
            throw new IllegalArgumentException("The selected record does not have a valid database identifier.");
        }
        return id.getLeastSignificantBits();
    }

    private static void requireSize(String moduleId, List<String> values, int expected) {
        if (values == null || values.size() != expected || values.stream().anyMatch(value -> value == null)) {
            throw new IllegalArgumentException(moduleId + " requires " + expected + " non-null field values.");
        }
    }

    private static boolean isActive(String value) {
        return value != null && (value.equalsIgnoreCase("ACTIVE")
                || value.equalsIgnoreCase("Active") || value.equalsIgnoreCase("true")
                || value.equalsIgnoreCase("Yes") || value.equalsIgnoreCase("Đang hoạt động"));
    }

    static boolean parseStatus(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "active", "true", "yes", "có", "đang hoạt động", "đang bán", "đang hợp tác" -> true;
            case "inactive", "false", "no", "không", "ngừng hoạt động", "ngừng bán", "tạm ngưng",
                    "ngừng hợp tác" -> false;
            default -> throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ: \"" + value + "\". Nhập ACTIVE hoặc INACTIVE.");
        };
    }

    static boolean parsePrescription(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "yes", "true", "required", "có", "kê đơn" -> true;
            case "no", "false", "không", "không kê đơn" -> false;
            default -> throw new IllegalArgumentException(
                    "Giá trị \"Kê đơn\" không hợp lệ: \"" + value + "\". Nhập Có hoặc Không.");
        };
    }

    private static RuntimeException friendly(SQLException exception, String fallback) {
        return switch (exception.getErrorCode()) {
            case 1062 -> new IllegalArgumentException(Localization.text(
                    exception.getMessage() != null && exception.getMessage().contains("uq_customers_phone")
                            ? "Số điện thoại này đã được dùng cho một khách hàng."
                            : "Dữ liệu bị trùng: mã hoặc tên này đã tồn tại."), exception);
            case 1451 -> new IllegalArgumentException(
                    "Không thể xóa vì bản ghi đang được sử dụng (sản phẩm, phiếu nhập, hóa đơn...). "
                            + "Hãy chuyển trạng thái sang INACTIVE thay vì xóa.", exception);
            case 1452 -> new IllegalArgumentException(
                    "Dữ liệu liên quan không tồn tại hoặc đã bị xóa.", exception);
            default -> DataAccessException.fromSql(exception, fallback);
        };
    }

    private static boolean affirmative(String value) {
        return value != null && (value.equalsIgnoreCase("YES") || value.equalsIgnoreCase("TRUE")
                || value.equalsIgnoreCase("REQUIRED") || value.equalsIgnoreCase("Có"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static int parseNonNegativeInt(String value, String label) {
        try {
            int parsed = Integer.parseInt(value.replace(",", "").replace(".", "").trim());
            if (parsed < 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a non-negative integer.", exception);
        }
    }

    private static UnsupportedOperationException readOnly(String moduleId) {
        return new UnsupportedOperationException(moduleId + " is read-only in this UI because its operation "
                + "requires related records/transactions that this form does not collect.");
    }

    private static UUID loyaltyEventRowId(long saleId, boolean redeemed) {
        return new UUID(saleId, redeemed ? 0x52454445454dL : 0x4541524e4544L);
    }

    private record CustomerAllergy(String name, String note) {
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T read(ResultSet resultSet) throws SQLException;
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
