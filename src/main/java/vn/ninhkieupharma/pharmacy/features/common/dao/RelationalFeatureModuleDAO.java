package vn.ninhkieupharma.pharmacy.features.common.dao;

import vn.ninhkieupharma.pharmacy.database.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.features.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.auth.service.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Maps the feature screens to the normalized tables in 01_schema_quan_ly_cua_hang.sql.
 * The application deliberately does not own schema creation or demo business data.
 */
public final class RelationalFeatureModuleDAO implements FeatureModuleDAO {
    private static final long ROW_ID_PREFIX = 0x4e4b504841524d41L;

    @Override
    public boolean supportsNativeReports() {
        return true;
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
                    SELECT p.product_id, p.product_code, p.product_name, c.category_name, p.unit_name,
                           IF(p.requires_prescription, 'Yes', 'No'), IF(p.is_active, 'ACTIVE', 'INACTIVE'),
                           COALESCE((SELECT MIN(s.supplier_name) FROM supplier_products sp
                                     JOIN suppliers s ON s.supplier_id = sp.supplier_id
                                     WHERE sp.product_id = p.product_id), '')
                    FROM products p
                    JOIN product_categories c ON c.category_id = p.category_id
                    ORDER BY p.product_id
                    """);
            case "UC-09" -> query(moduleId, """
                    SELECT supplier_id, supplier_code, supplier_name, COALESCE(phone, ''),
                           COALESCE(email, ''), IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM suppliers ORDER BY supplier_id
                    """);
            case "UC-10" -> query(moduleId, """
                    SELECT c.category_id, c.category_name, COUNT(p.product_id),
                           CONCAT(ROUND(100.0 * COUNT(p.product_id) /
                               NULLIF((SELECT COUNT(*) FROM products), 0), 1), '%'),
                           IF(COUNT(p.product_id) > 0, 'Has products', 'Empty')
                    FROM product_categories c
                    LEFT JOIN products p ON p.category_id = c.category_id
                    GROUP BY c.category_id, c.category_name
                    ORDER BY c.category_name
                    """);
            case "UC-11" -> query(moduleId, """
                    SELECT s.supplier_id, s.supplier_name, COUNT(sp.product_id),
                           CONCAT(ROUND(100.0 * COUNT(sp.product_id) /
                               NULLIF((SELECT COUNT(*) FROM supplier_products), 0), 1), '%'),
                           IF(COUNT(sp.product_id) > 1, 'Multiple products', 'Limited coverage')
                    FROM suppliers s
                    LEFT JOIN supplier_products sp ON sp.supplier_id = s.supplier_id
                    GROUP BY s.supplier_id, s.supplier_name
                    ORDER BY s.supplier_name
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
                           COALESCE((SELECT GROUP_CONCAT(ca.allergen_name ORDER BY ca.allergen_name
                               SEPARATOR ', ') FROM customer_allergies ca
                               WHERE ca.customer_id = c.customer_id), ''),
                           c.loyalty_points, IF(c.is_active, 'ACTIVE', 'INACTIVE')
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
                           CONCAT(discount_percent, '%'),
                           CONCAT(DATE_FORMAT(starts_at, '%Y-%m-%d'), ' to ',
                                  DATE_FORMAT(ends_at, '%Y-%m-%d')),
                           IF(is_active, 'ACTIVE', 'INACTIVE')
                    FROM promotions ORDER BY promotion_id
                    """);
            case "UC-19" -> query(moduleId, """
                    SELECT DATE(sold_at), COUNT(*), CAST(ROUND(SUM(total_amount), 0) AS UNSIGNED),
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
        String sql = switch (moduleId) {
            case "UC-02" -> "DELETE FROM employee_availabilities WHERE availability_id = ?";
            case "UC-07" -> "DELETE FROM product_categories WHERE category_id = ?";
            case "UC-08" -> "DELETE FROM products WHERE product_id = ?";
            case "UC-09" -> "DELETE FROM suppliers WHERE supplier_id = ?";
            case "UC-13" -> "DELETE FROM warehouse_locations WHERE location_id = ?";
            case "UC-16" -> "DELETE FROM customers WHERE customer_id = ?";
            case "UC-18" -> "DELETE FROM promotions WHERE promotion_id = ?";
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
        long id = insert("""
                INSERT INTO product_categories (category_name, description, is_active)
                VALUES (?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setBoolean(3, isActive(values.get(2)));
        });
        return row(id, values);
    }

    private ModuleRow updateCategory(long id, List<String> values) {
        requireSize("UC-07", values, 3);
        execute("""
                UPDATE product_categories SET category_name = ?, description = ?, is_active = ?
                WHERE category_id = ?
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setBoolean(3, isActive(values.get(2)));
            statement.setLong(4, id);
        });
        return row(id, values);
    }

    private ModuleRow createProduct(List<String> values) {
        requireSize("UC-08", values, 7);
        long categoryId = resolveId("SELECT category_id FROM product_categories WHERE category_name = ?",
                values.get(2));
        long productId = transaction(connection -> {
            long id = insert(connection, """
                    INSERT INTO products
                        (category_id, product_code, product_name, dosage_form, unit_name,
                         requires_prescription, is_active)
                    VALUES (?, ?, ?, NULL, ?, ?, ?)
                    """, statement -> {
                statement.setLong(1, categoryId);
                statement.setString(2, values.get(0));
                statement.setString(3, values.get(1));
                statement.setString(4, values.get(3));
                statement.setBoolean(5, affirmative(values.get(4)));
                statement.setBoolean(6, isActive(values.get(5)));
            });
            requireNoUnpricedSupplier(values.get(6));
            return id;
        });
        return row(productId, values);
    }

    private ModuleRow updateProduct(long id, List<String> values) {
        requireSize("UC-08", values, 7);
        long categoryId = resolveId("SELECT category_id FROM product_categories WHERE category_name = ?",
                values.get(2));
        transaction(connection -> {
            execute(connection, """
                    UPDATE products SET category_id = ?, product_code = ?, product_name = ?,
                        unit_name = ?, requires_prescription = ?, is_active = ?
                    WHERE product_id = ?
                    """, statement -> {
                statement.setLong(1, categoryId);
                statement.setString(2, values.get(0));
                statement.setString(3, values.get(1));
                statement.setString(4, values.get(3));
                statement.setBoolean(5, affirmative(values.get(4)));
                statement.setBoolean(6, isActive(values.get(5)));
                statement.setLong(7, id);
            });
            requireExistingSupplierLink(connection, id, values.get(6));
            return null;
        });
        return row(id, values);
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
        requireSize("UC-09", values, 5);
        long id = insert("""
                INSERT INTO suppliers (supplier_code, supplier_name, phone, email, is_active)
                VALUES (?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setString(3, values.get(2));
            statement.setString(4, values.get(3));
            statement.setBoolean(5, isActive(values.get(4)));
        });
        return row(id, values);
    }

    private ModuleRow updateSupplier(long id, List<String> values) {
        requireSize("UC-09", values, 5);
        execute("""
                UPDATE suppliers SET supplier_code = ?, supplier_name = ?, phone = ?, email = ?, is_active = ?
                WHERE supplier_id = ?
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setString(3, values.get(2));
            statement.setString(4, values.get(3));
            statement.setBoolean(5, isActive(values.get(4)));
            statement.setLong(6, id);
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
        requireSize("UC-16", values, 6);
        long id = transaction(connection -> {
            String code = values.get(0).isBlank()
                    ? "KH" + Long.toUnsignedString(System.nanoTime(), 36).toUpperCase(Locale.ROOT)
                    : values.get(0);
            long customerId = insert(connection, """
                    INSERT INTO customers
                        (customer_code, full_name, phone, loyalty_points, is_active)
                    VALUES (?, ?, ?, ?, ?)
                    """, statement -> {
                statement.setString(1, code);
                statement.setString(2, values.get(1));
                statement.setString(3, blankToNull(values.get(2)));
                statement.setInt(4, parseNonNegativeInt(values.get(4), "Loyalty points"));
                statement.setBoolean(5, isActive(values.get(5)));
            });
            replaceAllergies(connection, customerId, values.get(3));
            return customerId;
        });
        return row(id, values);
    }

    private ModuleRow updateCustomer(long id, List<String> values) {
        requireSize("UC-16", values, 6);
        transaction(connection -> {
            execute(connection, """
                    UPDATE customers SET customer_code = ?, full_name = ?, phone = ?,
                        loyalty_points = ?, is_active = ? WHERE customer_id = ?
                    """, statement -> {
                statement.setString(1, values.get(0));
                statement.setString(2, values.get(1));
                statement.setString(3, blankToNull(values.get(2)));
                statement.setInt(4, parseNonNegativeInt(values.get(4), "Loyalty points"));
                statement.setBoolean(5, isActive(values.get(5)));
                statement.setLong(6, id);
            });
            replaceAllergies(connection, id, values.get(3));
            return null;
        });
        return row(id, values);
    }

    private void replaceAllergies(Connection connection, long customerId, String allergyNames) throws SQLException {
        execute(connection, "DELETE FROM customer_allergies WHERE customer_id = ?",
                statement -> statement.setLong(1, customerId));
        if (allergyNames == null || allergyNames.isBlank()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO customer_allergies (customer_id, allergen_name) VALUES (?, ?)")) {
            for (String allergy : allergyNames.split(",")) {
                String name = allergy.trim();
                if (!name.isEmpty()) {
                    statement.setLong(1, customerId);
                    statement.setString(2, name);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private ModuleRow createPromotion(List<String> values) {
        requireSize("UC-18", values, 5);
        LocalDate[] dates = parseDateRange(values.get(3));
        long id = insert("""
                INSERT INTO promotions
                    (promotion_code, promotion_name, discount_percent, starts_at, ends_at, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setBigDecimal(3, parsePercent(values.get(2)));
            statement.setString(4, dates[0] + " 00:00:00");
            statement.setString(5, dates[1] + " 23:59:59");
            statement.setBoolean(6, isActive(values.get(4)));
        });
        return row(id, values);
    }

    private ModuleRow updatePromotion(long id, List<String> values) {
        requireSize("UC-18", values, 5);
        LocalDate[] dates = parseDateRange(values.get(3));
        execute("""
                UPDATE promotions SET promotion_code = ?, promotion_name = ?, discount_percent = ?,
                    starts_at = ?, ends_at = ?, is_active = ? WHERE promotion_id = ?
                """, statement -> {
            statement.setString(1, values.get(0));
            statement.setString(2, values.get(1));
            statement.setBigDecimal(3, parsePercent(values.get(2)));
            statement.setString(4, dates[0] + " 00:00:00");
            statement.setString(5, dates[1] + " 23:59:59");
            statement.setBoolean(6, isActive(values.get(4)));
            statement.setLong(7, id);
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
            java.math.BigDecimal parsed = new java.math.BigDecimal(value.replace("%", "").trim());
            if (parsed.signum() < 0 || parsed.compareTo(new java.math.BigDecimal("100")) > 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Discount must be between 0 and 100 percent.", exception);
        }
    }

    private static LocalDate[] parseDateRange(String value) {
        String[] parts = value.split("\\s+to\\s+", 2);
        if (parts.length != 2) {
            parts = value.split("\\s+đến\\s+", 2);
        }
        try {
            if (parts.length != 2) {
                throw new IllegalArgumentException("Enter a date range as YYYY-MM-DD to YYYY-MM-DD.");
            }
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("Promotion end date must be after its start date.");
            }
            return new LocalDate[]{start, end};
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Promotion dates must use YYYY-MM-DD.", exception);
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
