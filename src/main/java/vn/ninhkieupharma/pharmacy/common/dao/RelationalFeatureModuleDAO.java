package vn.ninhkieupharma.pharmacy.common.dao;

import vn.ninhkieupharma.pharmacy.config.DatabaseConnection;
import vn.ninhkieupharma.pharmacy.common.model.ModuleRow;
import vn.ninhkieupharma.pharmacy.common.auth.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        final String roleCode = normalizeRoleCode(values.get(5));
        String passwordHash = PasswordHasher.hash(values.get(6));
        long employeeId = transaction(connection -> {
            long roleId = resolveId(connection, "SELECT role_id FROM roles WHERE role_code = ?", roleCode);
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
                values.get(4).trim(), "ADMIN".equals(roleCode) ? "Quản trị viên" : "Nhân viên", "Đang hoạt động"));
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
        String sql = switch (moduleId) {
            case "UC-02" -> "DELETE FROM employee_availabilities WHERE availability_id = ?";
            case "UC-03" -> "DELETE FROM schedule_assignments WHERE assignment_id = ?";
            case "UC-04" -> "DELETE FROM attendance_records WHERE attendance_id = ?";
            case "UC-07" -> "DELETE FROM product_categories WHERE category_id = ?";
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
        requireSize("UC-08", values, 9);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(2).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm, tên sản phẩm, danh mục và đơn vị tính là bắt buộc.");
        }
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
        requireSize("UC-08", values, 9);
        if (values.get(0).isBlank() || values.get(1).isBlank() || values.get(2).isBlank() || values.get(4).isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm, tên sản phẩm, danh mục và đơn vị tính là bắt buộc.");
        }
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

    static java.math.BigDecimal parseUnitPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            String cleaned = raw.replace("VNĐ", "").replace("VND", "").replace("đ", "")
                    .replaceAll("\\s+", "");
            if (cleaned.matches("\\d+[.,]\\d{1,2}")) {
                cleaned = cleaned.replace(',', '.');
            } else {
                cleaned = cleaned.replace(".", "").replace(",", "");
            }
            java.math.BigDecimal price = new java.math.BigDecimal(cleaned)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Giá bán không được âm.");
            }
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
        requireSize("UC-16", values, 9);
        validateCustomer(values);
        List<CustomerAllergy> allergies = parseCustomerAllergies(values.get(4));
        String status = normalizeCustomerStatus(values.get(6));
        long id = transaction(connection -> {
            String code = values.get(0).isBlank()
                    ? "KH" + Long.toUnsignedString(System.nanoTime(), 36).toUpperCase(Locale.ROOT)
                    : values.get(0);
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
                statement.setInt(6, parseNonNegativeInt(values.get(5), "Điểm tích lũy"));
                statement.setBoolean(7, customerIsActive(status));
                statement.setBoolean(8, customerIsBlocked(status));
                statement.setString(9, customerIsBlocked(status) ? blankToNull(values.get(7)) : null);
            });
            replaceAllergies(connection, customerId, allergies);
            return customerId;
        });
        return row(id, values);
    }

    private ModuleRow updateCustomer(long id, List<String> values) {
        requireSize("UC-16", values, 9);
        validateCustomer(values);
        List<CustomerAllergy> allergies = parseCustomerAllergies(values.get(4));
        String status = normalizeCustomerStatus(values.get(6));
        transaction(connection -> {
            execute(connection, """
                    UPDATE customers SET customer_code = ?, full_name = ?, phone = ?, email = ?,
                        date_of_birth = ?, loyalty_points = ?, is_active = ?, is_blocked = ?,
                        block_reason = ? WHERE customer_id = ?
                    """, statement -> {
                statement.setString(1, values.get(0));
                statement.setString(2, values.get(1));
                statement.setString(3, blankToNull(values.get(2)));
                statement.setString(4, blankToNull(values.get(3)));
                setCustomerDate(statement, 5, values.get(8));
                statement.setInt(6, parseNonNegativeInt(values.get(5), "Điểm tích lũy"));
                statement.setBoolean(7, customerIsActive(status));
                statement.setBoolean(8, customerIsBlocked(status));
                statement.setString(9, customerIsBlocked(status) ? blankToNull(values.get(7)) : null);
                statement.setLong(10, id);
            });
            replaceAllergies(connection, id, allergies);
            return null;
        });
        return row(id, values);
    }

    private void validateCustomer(List<String> values) {
        if (values.get(1).isBlank()) {
            throw new IllegalArgumentException("Tên khách hàng là bắt buộc.");
        }
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
        String status = normalizeCustomerStatus(values.get(6));
        if (customerIsBlocked(status) && values.get(7).isBlank()) {
            throw new IllegalArgumentException("Cần nhập lý do khi hạn chế bán.");
        }
        parseNonNegativeInt(values.get(5), "Điểm tích lũy");
        parseCustomerAllergies(values.get(4));
        if (!values.get(8).isBlank()) {
            try {
                LocalDate.parse(values.get(8));
            } catch (java.time.format.DateTimeParseException exception) {
                throw new IllegalArgumentException("Ngày sinh phải theo định dạng YYYY-MM-DD.", exception);
            }
        }
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
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062 || exception.getErrorCode() == 1451
                || exception.getErrorCode() == 1452) {
                throw friendly(exception, "");
            }
            String detail = exception.getMessage();
            String message = "Could not complete the related business-data transaction "
                    + "(SQLState " + exception.getSQLState() + ", error " + exception.getErrorCode() + ")"
                    + (detail == null || detail.isBlank() ? "." : ": " + detail);
            throw new DataAccessException(message, exception);
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
            case 1062 -> new IllegalArgumentException(
                    "Dữ liệu bị trùng: mã hoặc tên này đã tồn tại.", exception);
            case 1451 -> new IllegalArgumentException(
                    "Không thể xóa vì bản ghi đang được sử dụng (sản phẩm, phiếu nhập, hóa đơn...). "
                            + "Hãy chuyển trạng thái sang INACTIVE thay vì xóa.", exception);
            case 1452 -> new IllegalArgumentException(
                    "Dữ liệu liên quan không tồn tại hoặc đã bị xóa.", exception);
            default -> new DataAccessException(fallback, exception);
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

    private record CustomerAllergy(String name, String note) {
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
