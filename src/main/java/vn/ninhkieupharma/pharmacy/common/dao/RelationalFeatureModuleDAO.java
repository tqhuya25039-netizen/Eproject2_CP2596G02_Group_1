package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.auth.PasswordHasher;
import vn.ninhkieupharma.pharmacy.common.service.Localization;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleAllergyWarning;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleOrderRequest;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleProduct;
import vn.ninhkieupharma.pharmacy.domain.sales.SaleReceipt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps the feature screens to the normalized tables in environment/01_schema_quan_ly_cua_hang.sql.
 * The application deliberately does not own schema creation or demo business data.
 */
public final class RelationalFeatureModuleDAO implements FeatureModuleDAO {
    private static final long ROW_ID_PREFIX = 0x4e4b504841524d41L;
    private final SalesCheckoutDAO salesCheckoutDAO = new SalesCheckoutDAO();

    @Override
    public List<SaleProduct> findSaleProducts() {
        return salesCheckoutDAO.findSaleProducts();
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

    private record CustomerState(boolean active, boolean blocked) {
        private String code() {
            if (!active && blocked) {
                return "ARCHIVED_BLOCKED";
            }
            if (!active) {
                return "ARCHIVED";
            }
            return blocked ? "BLOCKED" : "ACTIVE";
        }
    }

    private record CustomerAllergy(String name, String note) {
    }

    @Override
    public boolean supportsNativeReports() {
        return true;
    }

    @Override
    public List<ModuleRow> findCustomerPurchaseHistory(UUID customerRowId) {
        long customerId = numericId(customerRowId);
        String sql = """
                SELECT sale_id, sale_code, DATE_FORMAT(sold_at, '%Y-%m-%d %H:%i'),
                       CAST(ROUND(total_amount, 0) AS CHAR), status
                FROM sales
                WHERE customer_id = ?
                ORDER BY sold_at DESC, sale_id DESC
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
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
            throw new DataAccessException("Could not load customer purchase history from MySQL.", exception);
        }
    }

    @Override
    public List<ModuleRow> findCustomersAvailableForSale() {
        String sql = """
                SELECT c.customer_id, c.full_name, COALESCE(c.phone, ''), COALESCE(c.email, ''),
                       COALESCE((SELECT GROUP_CONCAT(
                           CONCAT(ca.allergen_name,
                               IF(ca.note IS NULL OR ca.note = '', '', CONCAT(' | ', ca.note)))
                           ORDER BY ca.allergen_name SEPARATOR '\\n')
                           FROM customer_allergies ca
                           WHERE ca.customer_id = c.customer_id), ''),
                       c.loyalty_points, COALESCE(DATE_FORMAT(c.date_of_birth, '%Y-%m-%d'), '')
                FROM customers c
                WHERE c.is_active = 1 AND c.is_blocked = 0
                ORDER BY c.full_name, c.customer_id
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            List<ModuleRow> customers = new ArrayList<>();
            while (resultSet.next()) {
                customers.add(row(resultSet.getLong("customer_id"), List.of(
                        resultSet.getString("full_name"),
                        resultSet.getString(3),
                        resultSet.getString(4),
                        resultSet.getString(5),
                        resultSet.getString("loyalty_points"),
                        resultSet.getString(6))));
            }
            return List.copyOf(customers);
        } catch (SQLException exception) {
            throw new DataAccessException("Could not load eligible customers for sales.", exception);
        }
    }

    @Override
    public ModuleRow createCustomerFromSale(List<String> values) {
        List<String> customerValues = salesCustomerValues(values);
        ModuleRow created = createCustomer(customerValues);
        return new ModuleRow(created.id(), saleCustomerValues(created.values()));
    }

    @Override
    public ModuleRow updateCustomerFromSale(UUID customerRowId, List<String> values) {
        List<String> customerValues = salesCustomerValues(values);
        long customerId = numericId(customerRowId);
        String phone = normalizeCustomerPhone(customerValues.get(2));
        long loyaltyPoints = transaction(connection -> {
            long currentPoints = ensureCustomerAvailableForSale(connection, customerId);
            ensureCustomerPhoneAvailable(connection, phone, customerId);
            ensureCustomerEmailAvailable(connection, customerValues.get(3), customerId);
            execute(connection, """
                    UPDATE customers
                    SET full_name = ?, phone = ?, email = ?, date_of_birth = ?
                    WHERE customer_id = ?
                    """, statement -> {
                statement.setString(1, customerValues.get(1).trim());
                setNullableString(statement, 2, phone);
                setNullableString(statement, 3, customerValues.get(3));
                setNullableDate(statement, 4, parseCustomerDate(customerValues.get(8)));
                statement.setLong(5, customerId);
            });
            replaceAllergies(connection, customerId, customerValues.get(4));
            return currentPoints;
        });
        return row(customerId, List.of(
                customerValues.get(1).trim(),
                phone,
                customerValues.get(3),
                customerValues.get(4),
                Long.toString(loyaltyPoints),
                customerValues.get(8)));
    }

    @Override
    public List<ModuleRow> findAll(String moduleId) {
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
                    SELECT e.employee_id, e.full_name, 'All time',
                           COUNT(DISTINCT a.assignment_id),
                           COALESCE(ROUND(SUM(
                               CASE WHEN ar.check_in_at IS NOT NULL AND ar.check_out_at IS NOT NULL
                                    THEN TIMESTAMPDIFF(MINUTE, ar.check_in_at, ar.check_out_at) / 60.0
                                    ELSE 0 END), 2), 0),
                           'Calculated from attendance'
                    FROM employees e
                    LEFT JOIN schedule_assignments a ON a.employee_id = e.employee_id
                    LEFT JOIN attendance_records ar ON ar.assignment_id = a.assignment_id
                    GROUP BY e.employee_id, e.full_name
                    ORDER BY e.full_name
                    """);
            case "UC-06" -> query(moduleId, """
                    SELECT ar.attendance_id, e.full_name, s.shift_name,
                           DATE_FORMAT(CONCAT(a.work_date, ' ', s.start_time), '%Y-%m-%d %H:%i'),
                           COALESCE(DATE_FORMAT(ar.check_in_at, '%Y-%m-%d %H:%i'), 'Not checked in'),
                           CASE WHEN ar.check_in_at IS NULL THEN 'Missing check-in'
                                WHEN ar.check_in_at <= TIMESTAMP(a.work_date, s.start_time)
                                    THEN 'On time'
                                ELSE CONCAT('Late ', TIMESTAMPDIFF(MINUTE,
                                    TIMESTAMP(a.work_date, s.start_time), ar.check_in_at), ' min') END
                    FROM attendance_records ar
                    JOIN schedule_assignments a ON a.assignment_id = ar.assignment_id
                    JOIN employees e ON e.employee_id = a.employee_id
                    JOIN shifts s ON s.shift_id = a.shift_id
                    ORDER BY a.work_date, e.full_name
                    """);
           case "UC-07" -> query(moduleId, """
                    SELECT category_id, category_name, COALESCE(description, ''),
                           IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM product_categories ORDER BY category_id
                    """);
            case "UC-08" -> query(moduleId, """
                    SELECT p.product_id, p.product_code, p.product_name, c.category_name,
                           COALESCE(p.dosage_form, ''), p.unit_name,
                           IF(p.requires_prescription, 'Yes', 'No'), IF(p.is_active, 'ACTIVE', 'INACTIVE'),
                           COALESCE((SELECT GROUP_CONCAT(DISTINCT s.supplier_name ORDER BY s.supplier_name SEPARATOR ', ')
                                     FROM supplier_products sp
                                     JOIN suppliers s ON s.supplier_id = sp.supplier_id
                                     WHERE sp.product_id = p.product_id), '')
                           , COALESCE(CAST(p.selling_price AS CHAR), '')
                           , COALESCE((SELECT GROUP_CONCAT(pi.ingredient_name
                               ORDER BY pi.ingredient_name SEPARATOR '; ')
                               FROM product_ingredients pi WHERE pi.product_id = p.product_id), '')
                    FROM products p
                    JOIN product_categories c ON c.category_id = p.category_id
                    ORDER BY p.product_id
                    """);
            case "UC-09" -> query(moduleId, """
                    SELECT supplier_id, supplier_code, supplier_name, COALESCE(phone, ''),
                           COALESCE(email, ''), COALESCE(address, ''), IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM suppliers ORDER BY supplier_id
                    """);
            case "UC-10" -> query(moduleId, """
                    SELECT (c.category_id * 100 + MAX(YEAR(p.created_at)) * 10 + MAX(QUARTER(p.created_at))) AS row_key,
                           CONCAT(MAX(YEAR(p.created_at)), '-Q', MAX(QUARTER(p.created_at))) AS quarter_label,
                           c.category_name,
                           CONCAT(COUNT(DISTINCT p.product_id), ' SP (',
                                  COUNT(DISTINCT COALESCE(NULLIF(p.dosage_form, ''), 'Mặc định')), ' dạng / ',
                                  COUNT(DISTINCT sp.supplier_id), ' NCC)'),
                           CONCAT(ROUND(100.0 * COUNT(DISTINCT p.product_id) /
                               NULLIF((SELECT COUNT(*) FROM products p2
                                       WHERE YEAR(p2.created_at) = MAX(YEAR(p.created_at))
                                         AND QUARTER(p2.created_at) = MAX(QUARTER(p.created_at))), 0), 1), '%'),
                           CASE
                               WHEN COUNT(DISTINCT p.product_id) >= 5 AND COUNT(DISTINCT sp.supplier_id) >= 2
                                   THEN 'Nhiều lựa chọn thay thế'
                               WHEN COUNT(DISTINCT p.product_id) >= 2
                                   THEN 'Có thể thay thế cơ bản'
                               ELSE 'Ít mặt hàng - Cần mở rộng'
                           END
                    FROM product_categories c
                    JOIN products p ON p.category_id = c.category_id AND p.is_active = 1
                    LEFT JOIN supplier_products sp ON sp.product_id = p.product_id
                    GROUP BY YEAR(p.created_at), QUARTER(p.created_at), c.category_id, c.category_name
                    ORDER BY quarter_label DESC, COUNT(DISTINCT p.product_id) DESC
                    """);
            case "UC-11" -> query(moduleId, """
                    SELECT (s.supplier_id * 10000 + MAX(YEAR(gr.received_at)) * 100 + MAX(MONTH(gr.received_at))) AS row_key,
                           DATE_FORMAT(gr.received_at, '%Y-%m') AS month_label,
                           s.supplier_name,
                           COUNT(DISTINCT ib.product_id) AS item_count,
                           CONCAT(ROUND(100.0 * COUNT(DISTINCT ib.product_id) /
                               NULLIF((SELECT COUNT(DISTINCT ib2.product_id)
                                       FROM goods_receipts gr2
                                       JOIN goods_receipt_items gri2 ON gri2.receipt_id = gr2.receipt_id
                                       JOIN inventory_batches ib2 ON ib2.batch_id = gri2.batch_id
                                       WHERE gr2.status = 'RECEIVED'
                                         AND DATE_FORMAT(gr2.received_at, '%Y-%m') = month_label), 0), 1), '%'),
                           CASE
                               WHEN (1.0 * COUNT(DISTINCT ib.product_id) /
                                   NULLIF((SELECT COUNT(DISTINCT ib2.product_id)
                                           FROM goods_receipts gr2
                                           JOIN goods_receipt_items gri2 ON gri2.receipt_id = gr2.receipt_id
                                           JOIN inventory_batches ib2 ON ib2.batch_id = gri2.batch_id
                                           WHERE gr2.status = 'RECEIVED'
                                             AND DATE_FORMAT(gr2.received_at, '%Y-%m') = month_label), 0)) >= 0.50
                                   THEN 'Phụ thuộc lớn (>= 50%)'
                               WHEN (1.0 * COUNT(DISTINCT ib.product_id) /
                                   NULLIF((SELECT COUNT(DISTINCT ib2.product_id)
                                           FROM goods_receipts gr2
                                           JOIN goods_receipt_items gri2 ON gri2.receipt_id = gr2.receipt_id
                                           JOIN inventory_batches ib2 ON ib2.batch_id = gri2.batch_id
                                           WHERE gr2.status = 'RECEIVED'
                                             AND DATE_FORMAT(gr2.received_at, '%Y-%m') = month_label), 0)) >= 0.25
                                   THEN 'Tỷ trọng trung bình'
                               ELSE 'Phân bổ đều / Thấp'
                           END
                    FROM goods_receipts gr
                    JOIN suppliers s ON s.supplier_id = gr.supplier_id
                    JOIN goods_receipt_items gri ON gri.receipt_id = gr.receipt_id
                    JOIN inventory_batches ib ON ib.batch_id = gri.batch_id
                    WHERE gr.status = 'RECEIVED'
                    GROUP BY DATE_FORMAT(gr.received_at, '%Y-%m'), s.supplier_id, s.supplier_name
                    ORDER BY month_label DESC, item_count DESC
                    """);
            case "UC-12" -> query(moduleId, """
                    SELECT gr.receipt_id, gr.receipt_code, s.supplier_name,
                           DATE_FORMAT(gr.received_at, '%Y-%m-%d'),
                           COUNT(gri.receipt_item_id), gr.status
                    FROM goods_receipts gr
                    JOIN suppliers s ON s.supplier_id = gr.supplier_id
                    LEFT JOIN goods_receipt_items gri ON gri.receipt_id = gr.receipt_id
                    GROUP BY gr.receipt_id, gr.receipt_code, s.supplier_name, gr.received_at, gr.status
                    ORDER BY gr.received_at DESC
                    """);
            case "UC-13" -> query(moduleId, """
                    SELECT location_id, location_code, location_name, COALESCE(description, ''),
                           IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM warehouse_locations ORDER BY location_id
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
                           im.movement_type, im.quantity_delta, COALESCE(im.note, '')
                    FROM inventory_movements im
                    JOIN inventory_batches b ON b.batch_id = im.batch_id
                    JOIN products p ON p.product_id = b.product_id
                    ORDER BY im.moved_at DESC
                    """);
            case "UC-16" -> query(moduleId, """
                    SELECT c.customer_id, c.customer_code, c.full_name, COALESCE(c.phone, ''),
                           COALESCE(c.email, ''),
                           COALESCE((SELECT GROUP_CONCAT(
                               CONCAT(ca.allergen_name,
                                   IF(ca.note IS NULL OR ca.note = '', '', CONCAT(' | ', ca.note)))
                               ORDER BY ca.allergen_name SEPARATOR '\\n') FROM customer_allergies ca
                               WHERE ca.customer_id = c.customer_id), ''),
                           c.loyalty_points,
                           CASE
                               WHEN NOT c.is_active AND c.is_blocked THEN 'ARCHIVED_BLOCKED'
                               WHEN NOT c.is_active THEN 'ARCHIVED'
                               WHEN c.is_blocked THEN 'BLOCKED'
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
            case "UC-18" -> query(moduleId, """
                    SELECT promotion_id, promotion_code, promotion_name,
                           CAST(discount_percent AS CHAR),
                           DATE_FORMAT(starts_at, '%Y-%m-%d'),
                           DATE_FORMAT(points_redemption_starts_at, '%Y-%m-%d'),
                           DATE_FORMAT(points_earning_ends_at, '%Y-%m-%d'),
                           DATE_FORMAT(ends_at, '%Y-%m-%d'),
                           CAST(redemption_vnd_per_point AS CHAR),
                           COALESCE((
                               SELECT GROUP_CONCAT(
                                   CONCAT(CAST(t.minimum_invoice_total_vnd AS CHAR), ':',
                                          CAST(t.points_rate_percent AS CHAR))
                                   ORDER BY t.minimum_invoice_total_vnd SEPARATOR ';')
                               FROM promotion_point_tiers t
                               WHERE t.promotion_id = p.promotion_id
                           ), ''),
                           IF(p.is_active, 'ACTIVE', 'INACTIVE')
                    FROM promotions p ORDER BY p.promotion_id
                    """);
            case "UC-19" -> query(moduleId, """
                    SELECT CAST(DATE_FORMAT(MIN(sold_at), '%Y%m%d') AS UNSIGNED),
                           DATE_FORMAT(MIN(sold_at), '%d/%m/%Y'), COUNT(*),
                           CAST(ROUND(SUM(total_amount), 0) AS UNSIGNED),
                           CAST(ROUND(SUM(discount_amount), 0) AS UNSIGNED),
                           CAST(ROUND(AVG(total_amount), 0) AS UNSIGNED)
                    FROM sales
                    WHERE status = 'COMPLETED'
                    GROUP BY DATE(sold_at)
                    ORDER BY DATE(sold_at)
                    """);
            default -> throw new IllegalArgumentException("Unknown feature module: " + moduleId);
        };
    }

    @Override
    public void replaceAll(String moduleId, List<List<String>> values) {
        throw new UnsupportedOperationException("Bulk replacement is disabled to protect existing business data.");
    }

    @Override
    public ModuleRow create(String moduleId, List<String> values) {
        return switch (moduleId) {
            case "UC-01" -> createEmployeeAccount(values);
            case "UC-02" -> createAvailability(values);
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
        if (!"STAFF".equalsIgnoreCase(values.get(5))) {
            throw new IllegalArgumentException("Tài khoản nhân viên mới chỉ được gán vai trò Nhân viên.");
        }
        String passwordHash = PasswordHasher.hash(values.get(6));
        long employeeId = transaction(connection -> {
            long roleId;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT role_id FROM roles WHERE role_code = 'STAFF'");
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Required STAFF role is missing from the roles table.");
                }
                roleId = resultSet.getLong("role_id");
            }
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

    @Override
    public ModuleRow update(String moduleId, UUID rowId, List<String> values) {
        long id = numericId(rowId);
        return switch (moduleId) {
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
        long id = numericId(rowId);
        if ("UC-16".equals(moduleId)) {
            executeOne("UPDATE customers SET is_active = 0 WHERE customer_id = ?", id);
            return;
        }
        String sql = switch (moduleId) {
            case "UC-02" -> "DELETE FROM employee_availabilities WHERE availability_id = ?";
            case "UC-07" -> "DELETE FROM product_categories WHERE category_id = ?";
            case "UC-08" -> "DELETE FROM products WHERE product_id = ?";
            case "UC-09" -> "DELETE FROM suppliers WHERE supplier_id = ?";
            case "UC-13" -> "DELETE FROM warehouse_locations WHERE location_id = ?";
            case "UC-18" -> "UPDATE promotions SET is_active = 0 WHERE promotion_id = ?";
            default -> throw readOnly(moduleId);
        };
        executeOne(sql, id);
    }

    private ModuleRow createAvailability(List<String> values) {
        requireSize("UC-02", values, 5);
        long employeeId = resolveId("SELECT employee_id FROM employees WHERE full_name = ? ORDER BY employee_id",
                values.get(0));
        long id = insert("""
                INSERT INTO employee_availabilities
                    (employee_id, available_date, start_time, end_time, note)
                VALUES (?, ?, ?, ?, ?)
                """, statement -> {
            statement.setLong(1, employeeId);
            statement.setString(2, values.get(1));
            statement.setString(3, values.get(2));
            statement.setString(4, values.get(3));
            statement.setString(5, values.get(4));
        });
        return row(id, values);
    }

    private ModuleRow updateAvailability(long id, List<String> values) {
        requireSize("UC-02", values, 5);
        long employeeId = resolveId("SELECT employee_id FROM employees WHERE full_name = ? ORDER BY employee_id",
                values.get(0));
        execute("""
                UPDATE employee_availabilities
                SET employee_id = ?, available_date = ?, start_time = ?, end_time = ?, note = ?
                WHERE availability_id = ?
                """, statement -> {
            statement.setLong(1, employeeId);
            statement.setString(2, values.get(1));
            statement.setString(3, values.get(2));
            statement.setString(4, values.get(3));
            statement.setString(5, values.get(4));
            statement.setLong(6, id);
        });
        return row(id, values);
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
        requireSize("UC-08", values, 10);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(2).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm, tên sản phẩm, danh mục và đơn vị tính là bắt buộc.");
        }
        BigDecimal sellingPrice = parseOptionalNonNegativeDecimal(values.get(8), "Selling price");
        long categoryId = resolveId("SELECT category_id FROM product_categories WHERE category_name = ?",
                values.get(2).trim());
        long productId = transaction(connection -> {
            long id = insert(connection, """
                    INSERT INTO products
                        (category_id, product_code, product_name, dosage_form, unit_name,
                         requires_prescription, selling_price, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, statement -> {
                statement.setLong(1, categoryId);
                statement.setString(2, values.get(0).trim());
                statement.setString(3, values.get(1).trim());
                setNullableString(statement, 4, values.get(3));
                statement.setString(5, values.get(4).trim());
                statement.setBoolean(6, affirmative(values.get(5)));
                setNullableBigDecimal(statement, 7, sellingPrice);
                statement.setBoolean(8, isActive(values.get(6)));
            });
            requireNoUnpricedSupplier(values.get(7));
            replaceProductIngredients(connection, id, values.get(9));
            return id;
        });
        return row(productId, values);
    }

    private ModuleRow updateProduct(long id, List<String> values) {
        requireSize("UC-08", values, 10);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(2).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm, tên sản phẩm, danh mục và đơn vị tính là bắt buộc.");
        }
        BigDecimal sellingPrice = parseOptionalNonNegativeDecimal(values.get(8), "Selling price");
        long categoryId = resolveId("SELECT category_id FROM product_categories WHERE category_name = ?",
                values.get(2).trim());
        transaction(connection -> {
            execute(connection, """
                    UPDATE products SET category_id = ?, product_code = ?, product_name = ?,
                        dosage_form = ?, unit_name = ?, requires_prescription = ?,
                        selling_price = ?, is_active = ?
                    WHERE product_id = ?
                    """, statement -> {
                statement.setLong(1, categoryId);
                statement.setString(2, values.get(0).trim());
                statement.setString(3, values.get(1).trim());
                setNullableString(statement, 4, values.get(3));
                statement.setString(5, values.get(4).trim());
                statement.setBoolean(6, affirmative(values.get(5)));
                setNullableBigDecimal(statement, 7, sellingPrice);
                statement.setBoolean(8, isActive(values.get(6)));
                statement.setLong(9, id);
            });
            requireExistingSupplierLink(connection, id, values.get(7));
            replaceProductIngredients(connection, id, values.get(9));
            return null;
        });
        return row(id, values);
    }

    private void replaceProductIngredients(Connection connection, long productId, String encodedIngredients)
            throws SQLException {
        List<String> ingredients = new ArrayList<>();
        java.util.Set<String> normalized = new java.util.HashSet<>();
        if (encodedIngredients != null && !encodedIngredients.isBlank()) {
            for (String ingredient : encodedIngredients.split(";", -1)) {
                String trimmed = ingredient.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.length() > 120 || !normalized.add(trimmed.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException(
                            "Ingredient names must be unique and no longer than 120 characters.");
                }
                ingredients.add(trimmed);
            }
        }
        execute(connection, "DELETE FROM product_ingredients WHERE product_id = ?",
                statement -> statement.setLong(1, productId));
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO product_ingredients (product_id, ingredient_name) VALUES (?, ?)")) {
            for (String ingredient : ingredients) {
                statement.setLong(1, productId);
                statement.setString(2, ingredient);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void requireNoUnpricedSupplier(String supplierName) {
        if (supplierName == null || supplierName.isBlank()) {
            return;
        }
        throw new UnsupportedOperationException("The product form does not collect supplier purchase price; "
                + "create the product without a supplier and use supplier-product maintenance separately.");
    }

    private void requireExistingSupplierLink(
            Connection connection, long productId, String supplierName) throws SQLException {
        if (supplierName == null || supplierName.isBlank()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM supplier_products WHERE product_id = ? LIMIT 1")) {
                statement.setLong(1, productId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        throw new UnsupportedOperationException("Removing supplier coverage is not supported "
                                + "by this form; use supplier-product maintenance.");
                    }
                }
            }
            return;
        }
        long supplierId = resolveId(connection,
                "SELECT supplier_id FROM suppliers WHERE supplier_name = ?", supplierName);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM supplier_products WHERE supplier_id = ? AND product_id = ?")) {
            statement.setLong(1, supplierId);
            statement.setLong(2, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new UnsupportedOperationException("Changing supplier coverage requires a purchase price, "
                            + "which this product form does not collect.");
                }
            }
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
        long id = insert("""
                INSERT INTO warehouse_locations (location_code, location_name, description, is_active)
                VALUES (?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setString(3, values.get(2));
            statement.setBoolean(4, isActive(values.get(3)));
        });
        return row(id, values);
    }

    private ModuleRow updateLocation(long id, List<String> values) {
        requireSize("UC-13", values, 4);
        execute("""
                UPDATE warehouse_locations SET location_code = ?, location_name = ?,
                    description = ?, is_active = ? WHERE location_id = ?
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setString(3, values.get(2));
            statement.setBoolean(4, isActive(values.get(3)));
            statement.setLong(5, id);
        });
        return row(id, values);
    }

    private ModuleRow createCustomer(List<String> values) {
        validateCustomer(values);
        CustomerState state = customerState(values.get(6));
        String phone = normalizeCustomerPhone(values.get(2));
        String code = "KH" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 18).toUpperCase(Locale.ROOT);
        long id = transaction(connection -> {
            ensureCustomerPhoneAvailable(connection, phone, 0);
            ensureCustomerEmailAvailable(connection, values.get(3), 0);
            long customerId = insert(connection, """
                    INSERT INTO customers
                        (customer_code, full_name, phone, email, date_of_birth, loyalty_points,
                         is_active, is_blocked, block_reason)
                    VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)
                    """, statement -> {
                statement.setString(1, code);
                statement.setString(2, values.get(1).trim());
                setNullableString(statement, 3, phone);
                setNullableString(statement, 4, values.get(3));
                setNullableDate(statement, 5, parseCustomerDate(values.get(8)));
                statement.setBoolean(6, state.active());
                statement.setBoolean(7, state.blocked());
                setNullableString(statement, 8, state.blocked() ? values.get(7).trim() : null);
            });
            replaceAllergies(connection, customerId, values.get(4));
            return customerId;
        });
        return row(id, customerValues(code, values, "0", state));
    }

    private ModuleRow updateCustomer(long id, List<String> values) {
        validateCustomer(values);
        CustomerState state = customerState(values.get(6));
        String phone = normalizeCustomerPhone(values.get(2));
        transaction(connection -> {
            ensureCustomerPhoneAvailable(connection, phone, id);
            ensureCustomerEmailAvailable(connection, values.get(3), id);
            execute(connection, """
                    UPDATE customers
                    SET full_name = ?, phone = ?, email = ?, is_active = ?, is_blocked = ?,
                        block_reason = ?, date_of_birth = ?
                    WHERE customer_id = ?
                    """, statement -> {
                statement.setString(1, values.get(1).trim());
                setNullableString(statement, 2, phone);
                setNullableString(statement, 3, values.get(3));
                statement.setBoolean(4, state.active());
                statement.setBoolean(5, state.blocked());
                setNullableString(statement, 6, state.blocked() ? values.get(7).trim() : null);
                setNullableDate(statement, 7, parseCustomerDate(values.get(8)));
                statement.setLong(8, id);
            });
            replaceAllergies(connection, id, values.get(4));
            return null;
        });
        return row(id, customerValues(values.get(0), values, values.get(5), state));
    }

    private static List<String> salesCustomerValues(List<String> values) {
        requireSize("UC-17", values, 5);
        List<String> customerValues = List.of(
                "",
                valueOrEmpty(values.get(0)),
                valueOrEmpty(values.get(1)),
                valueOrEmpty(values.get(2)),
                valueOrEmpty(values.get(3)),
                "0",
                "ACTIVE",
                "",
                valueOrEmpty(values.get(4)));
        validateCustomer(customerValues);
        return customerValues;
    }

    private static List<String> saleCustomerValues(List<String> customerValues) {
        return List.of(
                customerValues.get(1),
                customerValues.get(2),
                customerValues.get(3),
                customerValues.get(4),
                customerValues.get(5),
                customerValues.get(8));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void replaceAllergies(Connection connection, long customerId, String allergyText) throws SQLException {
        List<CustomerAllergy> allergies = parseCustomerAllergies(allergyText);
        java.util.Set<String> retainedNames = allergies.stream()
                .map(allergy -> allergy.name().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        List<String> existingNames = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT allergen_name FROM customer_allergies WHERE customer_id = ?")) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    existingNames.add(resultSet.getString(1));
                }
            }
        }
        for (String existingName : existingNames) {
            if (!retainedNames.contains(existingName.toLowerCase(Locale.ROOT))) {
                execute(connection,
                        "DELETE FROM customer_allergies WHERE customer_id = ? AND allergen_name = ?",
                        statement -> {
                            statement.setLong(1, customerId);
                            statement.setString(2, existingName);
                        });
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                """
                        INSERT INTO customer_allergies (customer_id, allergen_name, note)
                        VALUES (?, ?, ?)
                        ON DUPLICATE KEY UPDATE note = VALUES(note)
                        """)) {
            for (CustomerAllergy allergy : allergies) {
                statement.setLong(1, customerId);
                statement.setString(2, allergy.name());
                setNullableString(statement, 3, allergy.note());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private long ensureCustomerAvailableForSale(Connection connection, long customerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT loyalty_points, is_active, is_blocked
                FROM customers
                WHERE customer_id = ?
                FOR UPDATE
                """)) {
            statement.setLong(1, customerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new java.util.NoSuchElementException(
                            Localization.text("Không tìm thấy khách hàng."));
                }
                if (!resultSet.getBoolean("is_active") || resultSet.getBoolean("is_blocked")) {
                    throw new IllegalStateException(Localization.text(
                            "Chỉ có thể cập nhật khách hàng đang hoạt động và không bị hạn chế bán."));
                }
                return resultSet.getLong("loyalty_points");
            }
        }
    }

    private void ensureCustomerPhoneAvailable(Connection connection, String phone, long currentCustomerId)
            throws SQLException {
        if (phone.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM customers WHERE phone = ? AND customer_id <> ? LIMIT 1")) {
            statement.setString(1, phone);
            statement.setLong(2, currentCustomerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException(Localization.text(
                            "Số điện thoại đã được gắn với một khách hàng khác."));
                }
            }
        }
    }

    private void ensureCustomerEmailAvailable(Connection connection, String email, long currentCustomerId)
            throws SQLException {
        if (email == null || email.isBlank()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM customers WHERE LOWER(email) = LOWER(?) AND customer_id <> ? LIMIT 1")) {
            statement.setString(1, email.trim());
            statement.setLong(2, currentCustomerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException(Localization.text(
                            "Email đã được gắn với một khách hàng khác."));
                }
            }
        }
    }

    private static void validateCustomer(List<String> values) {
        requireSize("UC-16", values, 9);
        if (values.get(1).isBlank()) {
            throw new IllegalArgumentException(Localization.text("Tên khách hàng là bắt buộc."));
        }
        if (values.get(1).trim().length() > 120) {
            throw new IllegalArgumentException(Localization.text("Tên khách hàng không được vượt quá 120 ký tự."));
        }
        String phone = normalizeCustomerPhone(values.get(2));
        String email = values.get(3).trim();
        if (email.length() > 120 || (!email.isEmpty()
                && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new IllegalArgumentException(Localization.text(
                    "Email không hợp lệ hoặc vượt quá 120 ký tự."));
        }
        if (phone.isEmpty() && email.isEmpty()) {
            throw new IllegalArgumentException(Localization.text(
                    "Cần nhập số điện thoại hoặc email của khách hàng."));
        }
        parseCustomerDate(values.get(8));
        CustomerState state = customerState(values.get(6));
        if (state.blocked() && values.get(7).isBlank()) {
            throw new IllegalArgumentException(Localization.text(
                    "Cần nhập lý do khi hạn chế bán."));
        }
        if (state.blocked() && values.get(7).trim().length() > 255) {
            throw new IllegalArgumentException(Localization.text(
                    "Lý do hạn chế không được vượt quá 255 ký tự."));
        }
        parseCustomerAllergies(values.get(4));
    }

    private static String normalizeCustomerPhone(String phone) {
        String normalized = phone == null ? "" : phone.replaceAll("[\\s().-]", "");
        if (!normalized.isEmpty() && !normalized.matches("\\+?[0-9]{7,15}")) {
            throw new IllegalArgumentException(Localization.text(
                    "Số điện thoại cần có từ 7 đến 15 chữ số."));
        }
        return normalized;
    }

    private static List<CustomerAllergy> parseCustomerAllergies(String allergyText) {
        List<CustomerAllergy> allergies = new ArrayList<>();
        java.util.Set<String> names = new java.util.HashSet<>();
        if (allergyText == null || allergyText.isBlank()) {
            return allergies;
        }
        for (String line : allergyText.split("\\R")) {
            String[] parts = line.split("\\|", 2);
            String name = parts[0].trim();
            if (name.isEmpty()) {
                continue;
            }
            String note = parts.length == 2 ? parts[1].trim() : "";
            String key = name.toLowerCase(Locale.ROOT);
            if (name.length() > 120 || note.length() > 255) {
                throw new IllegalArgumentException(Localization.text(
                        "Tên dị ứng tối đa 120 ký tự và ghi chú tối đa 255 ký tự."));
            }
            if (!names.add(key)) {
                throw new IllegalArgumentException(Localization.text("Không nhập trùng dị ứng."));
            }
            allergies.add(new CustomerAllergy(name, note));
        }
        return List.copyOf(allergies);
    }

    private static CustomerState customerState(String value) {
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> new CustomerState(true, false);
            case "BLOCKED" -> new CustomerState(true, true);
            case "ARCHIVED" -> new CustomerState(false, false);
            case "ARCHIVED_BLOCKED" -> new CustomerState(false, true);
            default -> throw new IllegalArgumentException(Localization.text("Trạng thái khách hàng không hợp lệ."));
        };
    }

    private static List<String> customerValues(
            String customerCode, List<String> input, String points, CustomerState state) {
        String reason = state.blocked() ? input.get(7).trim() : "";
        return List.of(customerCode, input.get(1).trim(), normalizeCustomerPhone(input.get(2)),
                input.get(3).trim(), input.get(4), points, state.code(), reason, input.get(8));
    }

    private static LocalDate parseCustomerDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException(Localization.text("Ngày sinh không thể ở tương lai."));
            }
            return date;
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    Localization.text("Ngày sinh phải theo định dạng YYYY-MM-DD."), exception);
        }
    }

    private ModuleRow createPromotion(List<String> values) {
        requireSize("UC-18", values, 10);
        LocalDate[] dates = parsePromotionDates(values);
        BigDecimal redemptionRate = parsePositiveDecimal(values.get(7), "Redemption value per point");
        long id = transaction(connection -> {
            long promotionId = insert(connection, """
                    INSERT INTO promotions
                        (promotion_code, promotion_name, discount_percent, points_multiplier,
                         starts_at, points_redemption_starts_at, points_earning_ends_at, ends_at,
                         redemption_vnd_per_point, is_active)
                    VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?)
                    """, statement -> {
                statement.setString(1, values.get(0).trim());
                statement.setString(2, values.get(1).trim());
                statement.setBigDecimal(3, parsePercent(values.get(2)));
                statement.setString(4, dates[0] + " 00:00:00");
                statement.setString(5, dates[1] + " 00:00:00");
                statement.setString(6, dates[2] + " 23:59:59");
                statement.setString(7, dates[3] + " 23:59:59");
                statement.setBigDecimal(8, redemptionRate);
                statement.setBoolean(9, isActive(values.get(9)));
            });
            savePromotionTiers(connection, promotionId, values.get(8));
            return promotionId;
        });
        return row(id, values);
    }

    private ModuleRow updatePromotion(long id, List<String> values) {
        requireSize("UC-18", values, 10);
        LocalDate[] dates = parsePromotionDates(values);
        BigDecimal redemptionRate = parsePositiveDecimal(values.get(7), "Redemption value per point");
        transaction(connection -> {
            execute(connection, """
                    UPDATE promotions SET promotion_code = ?, promotion_name = ?, discount_percent = ?,
                        starts_at = ?, points_redemption_starts_at = ?, points_earning_ends_at = ?,
                        ends_at = ?, redemption_vnd_per_point = ?, is_active = ?
                    WHERE promotion_id = ?
                    """, statement -> {
                statement.setString(1, values.get(0).trim());
                statement.setString(2, values.get(1).trim());
                statement.setBigDecimal(3, parsePercent(values.get(2)));
                statement.setString(4, dates[0] + " 00:00:00");
                statement.setString(5, dates[1] + " 00:00:00");
                statement.setString(6, dates[2] + " 23:59:59");
                statement.setString(7, dates[3] + " 23:59:59");
                statement.setBigDecimal(8, redemptionRate);
                statement.setBoolean(9, isActive(values.get(9)));
                statement.setLong(10, id);
            });
            savePromotionTiers(connection, id, values.get(8));
            return null;
        });
        return row(id, values);
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
            throw new DataAccessException("Could not create the business record.", exception);
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
            throw new DataAccessException("Could not update the business record.", exception);
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

    private static void setNullableBigDecimal(
            PreparedStatement statement, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DECIMAL);
        } else {
            statement.setBigDecimal(index, value);
        }
    }

    private static void setNullableDate(
            PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.DATE);
        } else {
            statement.setDate(index, java.sql.Date.valueOf(value));
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
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Could not complete the related business-data transaction.", exception);
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

    private static java.math.BigDecimal parsePercent(String value) {
        try {
            java.math.BigDecimal parsed =
                    new java.math.BigDecimal(value.replace("%", "").trim().replace(',', '.'));
            if (parsed.signum() < 0 || parsed.compareTo(new java.math.BigDecimal("100")) > 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Discount must be between 0 and 100 percent.", exception);
        }
    }

    private static LocalDate[] parsePromotionDates(List<String> values) {
        try {
            LocalDate accrualStart = LocalDate.parse(values.get(3));
            LocalDate redemptionStart = LocalDate.parse(values.get(4));
            LocalDate accrualEnd = LocalDate.parse(values.get(5));
            LocalDate redemptionEnd = LocalDate.parse(values.get(6));
            if (accrualStart.isAfter(redemptionStart)
                    || redemptionStart.isAfter(accrualEnd)
                    || accrualEnd.isAfter(redemptionEnd)) {
                throw new IllegalArgumentException(
                        "Promotion dates must be ordered: accrual start, redemption start, "
                                + "accrual end, redemption end.");
            }
            return new LocalDate[]{accrualStart, redemptionStart, accrualEnd, redemptionEnd};
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Promotion dates must use YYYY-MM-DD.", exception);
        }
    }

    private static java.math.BigDecimal parsePositiveDecimal(String value, String label) {
        try {
            java.math.BigDecimal parsed = new java.math.BigDecimal(value.trim().replace(',', '.'));
            if (parsed.signum() <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be greater than zero.", exception);
        }
    }

    private static BigDecimal parseOptionalNonNegativeDecimal(String value, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim().replace(',', '.'))
                    .setScale(2, java.math.RoundingMode.UNNECESSARY);
            if (parsed.signum() < 0 || parsed.precision() > 12) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException(
                    label + " must be a non-negative amount with at most two decimal places.", exception);
        }
    }

    private void savePromotionTiers(Connection connection, long promotionId, String encodedTiers)
            throws SQLException {
        java.util.Map<java.math.BigDecimal, java.math.BigDecimal> tiers = new java.util.TreeMap<>();
        for (String encoded : encodedTiers.split(";", -1)) {
            String[] parts = encoded.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Each points tier must contain threshold:rate.");
            }
            try {
                java.math.BigDecimal threshold =
                        new java.math.BigDecimal(parts[0].trim().replace(',', '.'));
                java.math.BigDecimal rate =
                        new java.math.BigDecimal(parts[1].trim().replace(',', '.'));
                if (threshold.signum() < 0 || rate.signum() <= 0
                        || tiers.putIfAbsent(threshold, rate) != null) {
                    throw new IllegalArgumentException(
                            "Tier thresholds must be non-negative and unique; rates must be positive.");
                }
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Each tier must contain valid decimal values.", exception);
            }
        }

        execute(connection, "DELETE FROM promotion_point_tiers WHERE promotion_id = ?",
                statement -> statement.setLong(1, promotionId));
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO promotion_point_tiers
                    (promotion_id, minimum_invoice_total_vnd, points_rate_percent)
                VALUES (?, ?, ?)
                """)) {
            for (java.util.Map.Entry<java.math.BigDecimal, java.math.BigDecimal> tier : tiers.entrySet()) {
                statement.setLong(1, promotionId);
                statement.setBigDecimal(2, tier.getKey());
                statement.setBigDecimal(3, tier.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static UnsupportedOperationException readOnly(String moduleId) {
        return new UnsupportedOperationException(moduleId + " is read-only in this UI because its operation "
                + "requires related records/transactions that this form does not collect.");
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T run(Connection connection) throws SQLException;
    }
}
