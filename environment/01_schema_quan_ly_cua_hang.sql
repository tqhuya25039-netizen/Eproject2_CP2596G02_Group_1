-- CSDL quan ly cua hang: nhan su, san pham, kho va ban hang.
-- Yeu cau: MySQL 8.0.16+ (de ap dung day du cac CHECK constraint).
-- Chay tren mot database moi; khong chay lai tren database da co cac bang nay.

CREATE DATABASE IF NOT EXISTS quan_ly_cua_hang
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE quan_ly_cua_hang;
SET NAMES utf8mb4;

CREATE TABLE roles (
    role_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_code VARCHAR(30) NOT NULL,
    role_name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id),
    UNIQUE KEY uq_roles_role_code (role_code)
) ENGINE=InnoDB;

CREATE TABLE users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id BIGINT UNSIGNED NOT NULL,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_username (username),
    CONSTRAINT fk_users_role
        FOREIGN KEY (role_id) REFERENCES roles (role_id),
    CONSTRAINT chk_users_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE employees (
    employee_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    employee_code VARCHAR(20) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    hire_date DATE NOT NULL,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (employee_id),
    UNIQUE KEY uq_employees_user_id (user_id),
    UNIQUE KEY uq_employees_employee_code (employee_code),
    UNIQUE KEY uq_employees_email (email),
    CONSTRAINT fk_employees_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT chk_employees_status
        CHECK (employment_status IN ('ACTIVE', 'ON_LEAVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE shifts (
    shift_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    shift_name VARCHAR(80) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (shift_id),
    UNIQUE KEY uq_shifts_name_start (shift_name, start_time),
    CONSTRAINT chk_shifts_time CHECK (start_time < end_time),
    CONSTRAINT chk_shifts_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE employee_availabilities (
    availability_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    employee_id BIGINT UNSIGNED NOT NULL,
    available_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    note VARCHAR(255) NULL,
    PRIMARY KEY (availability_id),
    KEY ix_availability_employee_date (employee_id, available_date),
    CONSTRAINT fk_availability_employee
        FOREIGN KEY (employee_id) REFERENCES employees (employee_id),
    CONSTRAINT chk_availability_time CHECK (start_time < end_time)
) ENGINE=InnoDB;

CREATE TABLE work_schedules (
    schedule_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    week_start_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (schedule_id),
    UNIQUE KEY uq_work_schedules_week (week_start_date),
    CONSTRAINT fk_work_schedules_creator
        FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT chk_work_schedules_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE schedule_assignments (
    assignment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT UNSIGNED NOT NULL,
    employee_id BIGINT UNSIGNED NOT NULL,
    shift_id BIGINT UNSIGNED NOT NULL,
    work_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    note VARCHAR(255) NULL,
    PRIMARY KEY (assignment_id),
    UNIQUE KEY uq_assignment_slot (schedule_id, employee_id, work_date, shift_id),
    KEY ix_assignment_employee_date (employee_id, work_date),
    CONSTRAINT fk_assignments_schedule
        FOREIGN KEY (schedule_id) REFERENCES work_schedules (schedule_id),
    CONSTRAINT fk_assignments_employee
        FOREIGN KEY (employee_id) REFERENCES employees (employee_id),
    CONSTRAINT fk_assignments_shift
        FOREIGN KEY (shift_id) REFERENCES shifts (shift_id),
    CONSTRAINT chk_assignments_status
        CHECK (status IN ('PLANNED', 'CANCELLED', 'COMPLETED'))
) ENGINE=InnoDB;

CREATE TABLE attendance_records (
    attendance_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT UNSIGNED NOT NULL,
    check_in_at DATETIME NULL,
    check_out_at DATETIME NULL,
    note VARCHAR(255) NULL,
    PRIMARY KEY (attendance_id),
    UNIQUE KEY uq_attendance_assignment (assignment_id),
    CONSTRAINT fk_attendance_assignment
        FOREIGN KEY (assignment_id) REFERENCES schedule_assignments (assignment_id),
    CONSTRAINT chk_attendance_time
        CHECK (check_out_at IS NULL OR check_in_at IS NOT NULL AND check_out_at >= check_in_at)
) ENGINE=InnoDB;

CREATE TABLE product_categories (
    category_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_categories_name (category_name),
    CONSTRAINT chk_categories_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE suppliers (
    supplier_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    supplier_code VARCHAR(20) NOT NULL,
    supplier_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    address VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (supplier_id),
    UNIQUE KEY uq_suppliers_code (supplier_code),
    CONSTRAINT chk_suppliers_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE products (
    product_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    product_code VARCHAR(30) NOT NULL,
    barcode VARCHAR(50) NULL,
    product_name VARCHAR(180) NOT NULL,
    dosage_form VARCHAR(80) NULL,
    unit_price DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    unit_name VARCHAR(30) NOT NULL,
    requires_prescription TINYINT(1) NOT NULL DEFAULT 0,
    selling_price DECIMAL(12, 2) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    UNIQUE KEY uq_products_code (product_code),
    UNIQUE KEY uq_products_barcode (barcode),
    KEY ix_products_category (category_id),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories (category_id),
    CONSTRAINT chk_products_prescription CHECK (requires_prescription IN (0, 1)),
    CONSTRAINT chk_products_selling_price CHECK (selling_price IS NULL OR selling_price >= 0),
    CONSTRAINT chk_products_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE product_ingredients (
    product_id BIGINT UNSIGNED NOT NULL,
    ingredient_name VARCHAR(120) NOT NULL,
    PRIMARY KEY (product_id, ingredient_name),
    CONSTRAINT fk_product_ingredients_product
        FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE supplier_products (
    supplier_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    supplier_sku VARCHAR(50) NULL,
    purchase_price DECIMAL(12, 2) NOT NULL,
    lead_time_days SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (supplier_id, product_id),
    CONSTRAINT fk_supplier_products_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id),
    CONSTRAINT fk_supplier_products_product
        FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT chk_supplier_products_price CHECK (purchase_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE warehouse_locations (
    location_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    location_code VARCHAR(30) NOT NULL,
    location_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (location_id),
    UNIQUE KEY uq_locations_code (location_code),
    CONSTRAINT chk_locations_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE inventory_batches (
    batch_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id BIGINT UNSIGNED NOT NULL,
    location_id BIGINT UNSIGNED NOT NULL,
    batch_code VARCHAR(50) NOT NULL,
    expiry_date DATE NULL,
    quantity_on_hand DECIMAL(12, 3) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (batch_id),
    UNIQUE KEY uq_inventory_batch (product_id, location_id, batch_code),
    CONSTRAINT fk_batches_product
        FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT fk_batches_location
        FOREIGN KEY (location_id) REFERENCES warehouse_locations (location_id),
    CONSTRAINT chk_batches_quantity CHECK (quantity_on_hand >= 0)
) ENGINE=InnoDB;

CREATE TABLE goods_receipts (
    receipt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    receipt_code VARCHAR(30) NOT NULL,
    supplier_id BIGINT UNSIGNED NOT NULL,
    received_by BIGINT UNSIGNED NOT NULL,
    received_at DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    note VARCHAR(255) NULL,
    PRIMARY KEY (receipt_id),
    UNIQUE KEY uq_receipts_code (receipt_code),
    CONSTRAINT fk_receipts_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id),
    CONSTRAINT fk_receipts_user
        FOREIGN KEY (received_by) REFERENCES users (user_id),
    CONSTRAINT chk_receipts_status CHECK (status IN ('RECEIVED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE goods_receipt_items (
    receipt_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (receipt_item_id),
    UNIQUE KEY uq_receipt_batch (receipt_id, batch_id),
    CONSTRAINT fk_receipt_items_receipt
        FOREIGN KEY (receipt_id) REFERENCES goods_receipts (receipt_id),
    CONSTRAINT fk_receipt_items_batch
        FOREIGN KEY (batch_id) REFERENCES inventory_batches (batch_id),
    CONSTRAINT chk_receipt_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_receipt_items_cost CHECK (unit_cost >= 0)
) ENGINE=InnoDB;

CREATE TABLE stocktakes (
    stocktake_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    location_id BIGINT UNSIGNED NOT NULL,
    stocktake_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT UNSIGNED NOT NULL,
    note VARCHAR(255) NULL,
    PRIMARY KEY (stocktake_id),
    CONSTRAINT fk_stocktakes_location
        FOREIGN KEY (location_id) REFERENCES warehouse_locations (location_id),
    CONSTRAINT fk_stocktakes_user
        FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT chk_stocktakes_status
        CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED'))
) ENGINE=InnoDB;

CREATE TABLE stocktake_items (
    stocktake_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    stocktake_id BIGINT UNSIGNED NOT NULL,
    batch_id BIGINT UNSIGNED NOT NULL,
    expected_quantity DECIMAL(12, 3) NOT NULL,
    counted_quantity DECIMAL(12, 3) NOT NULL,
    difference_quantity DECIMAL(12, 3) NOT NULL,
    PRIMARY KEY (stocktake_item_id),
    UNIQUE KEY uq_stocktake_batch (stocktake_id, batch_id),
    CONSTRAINT fk_stocktake_items_stocktake
        FOREIGN KEY (stocktake_id) REFERENCES stocktakes (stocktake_id),
    CONSTRAINT fk_stocktake_items_batch
        FOREIGN KEY (batch_id) REFERENCES inventory_batches (batch_id),
    CONSTRAINT chk_stocktake_quantities
        CHECK (expected_quantity >= 0 AND counted_quantity >= 0),
    CONSTRAINT chk_stocktake_difference
        CHECK (difference_quantity = counted_quantity - expected_quantity)
) ENGINE=InnoDB;

CREATE TABLE customers (
    customer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_code VARCHAR(20) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(120) NULL,
    date_of_birth DATE NULL,
    loyalty_points INT UNSIGNED NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    is_blocked TINYINT(1) NOT NULL DEFAULT 0,
    block_reason VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customers_code (customer_code),
    UNIQUE KEY uq_customers_phone (phone),
    CONSTRAINT chk_customers_is_active CHECK (is_active IN (0, 1)),
    CONSTRAINT chk_customers_is_blocked CHECK (is_blocked IN (0, 1)),
    CONSTRAINT chk_customers_block_reason CHECK (
        (is_blocked = 0) OR (block_reason IS NOT NULL AND CHAR_LENGTH(TRIM(block_reason)) > 0)
    )
) ENGINE=InnoDB;

CREATE TABLE customer_allergies (
    customer_allergy_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id BIGINT UNSIGNED NOT NULL,
    allergen_name VARCHAR(120) NOT NULL,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_allergy_id),
    UNIQUE KEY uq_customer_allergen (customer_id, allergen_name),
    CONSTRAINT fk_allergies_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id)
) ENGINE=InnoDB;

CREATE TABLE loyalty_programs (
    loyalty_program_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    program_name VARCHAR(100) NOT NULL,
    points_per_1000_currency DECIMAL(10, 4) NOT NULL,
    currency_per_point DECIMAL(12, 2) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (loyalty_program_id),
    CONSTRAINT chk_loyalty_rates
        CHECK (points_per_1000_currency >= 0 AND currency_per_point >= 0),
    CONSTRAINT chk_loyalty_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE promotions (
    promotion_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    promotion_code VARCHAR(30) NOT NULL,
    promotion_name VARCHAR(120) NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    points_multiplier DECIMAL(8, 2) NOT NULL DEFAULT 1,
    starts_at DATETIME NOT NULL,
    points_redemption_starts_at DATETIME NOT NULL,
    points_earning_ends_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    redemption_vnd_per_point DECIMAL(20, 8) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (promotion_id),
    UNIQUE KEY uq_promotions_code (promotion_code),
    CONSTRAINT chk_promotions_discount CHECK (discount_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_promotions_multiplier CHECK (points_multiplier >= 0),
    CONSTRAINT chk_promotions_point_dates CHECK (
        starts_at <= points_redemption_starts_at
        AND points_redemption_starts_at <= points_earning_ends_at
        AND points_earning_ends_at <= ends_at
    ),
    CONSTRAINT chk_promotions_redemption_rate CHECK (redemption_vnd_per_point > 0),
    CONSTRAINT chk_promotions_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE promotion_point_tiers (
    promotion_point_tier_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    promotion_id BIGINT UNSIGNED NOT NULL,
    minimum_invoice_total_vnd DECIMAL(20, 2) NOT NULL,
    points_rate_percent DECIMAL(20, 8) NOT NULL,
    PRIMARY KEY (promotion_point_tier_id),
    UNIQUE KEY uq_promotion_point_tier_threshold (promotion_id, minimum_invoice_total_vnd),
    CONSTRAINT chk_promotion_point_tier_threshold CHECK (minimum_invoice_total_vnd >= 0),
    CONSTRAINT chk_promotion_point_tier_rate CHECK (points_rate_percent > 0),
    CONSTRAINT fk_promotion_point_tiers_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions (promotion_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE sales (
    sale_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sale_code VARCHAR(30) NOT NULL,
    customer_id BIGINT UNSIGNED NULL,
    employee_id BIGINT UNSIGNED NOT NULL,
    promotion_id BIGINT UNSIGNED NULL,
    sold_at DATETIME NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    promotion_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    points_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    points_earned INT UNSIGNED NOT NULL DEFAULT 0,
    points_redeemed INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    redeem_points BOOLEAN NOT NULL DEFAULT FALSE,
    requested_points INT UNSIGNED NULL,
    revision BIGINT UNSIGNED NOT NULL DEFAULT 0,
    prescription_code VARCHAR(80) NULL,
    prescription_date DATE NULL,
    prescribing_doctor VARCHAR(120) NULL,
    clinic VARCHAR(180) NULL,
    diagnosis VARCHAR(255) NULL,
    note VARCHAR(500) NULL,
    PRIMARY KEY (sale_id),
    UNIQUE KEY uq_sales_code (sale_code),
    KEY ix_sales_sold_at (sold_at),
    CONSTRAINT fk_sales_customer
        FOREIGN KEY (customer_id) REFERENCES customers (customer_id),
    CONSTRAINT fk_sales_employee
        FOREIGN KEY (employee_id) REFERENCES employees (employee_id),
    CONSTRAINT fk_sales_promotion
        FOREIGN KEY (promotion_id) REFERENCES promotions (promotion_id),
    CONSTRAINT chk_sales_amounts
        CHECK (subtotal >= 0 AND discount_amount >= 0 AND total_amount >= 0),
    CONSTRAINT chk_sales_total CHECK (total_amount = subtotal - discount_amount),
    CONSTRAINT chk_sales_status CHECK (status IN ('PENDING', 'COMPLETED', 'VOIDED'))
) ENGINE=InnoDB;

CREATE TABLE sale_items (
    sale_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sale_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    line_discount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL,
    dosage_instructions VARCHAR(255) NULL,
    treatment_days SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    PRIMARY KEY (sale_item_id),
    KEY ix_sale_items_sale (sale_id),
    KEY ix_sale_items_product (product_id),
    CONSTRAINT fk_sale_items_sale
        FOREIGN KEY (sale_id) REFERENCES sales (sale_id),
    CONSTRAINT fk_sale_items_product
        FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT chk_sale_items_values
        CHECK (quantity > 0 AND unit_price >= 0 AND line_discount >= 0 AND line_total >= 0),
    CONSTRAINT chk_sale_items_total
        CHECK (line_total = ROUND(quantity * unit_price - line_discount, 2)),
    CONSTRAINT chk_sale_items_treatment_days CHECK (treatment_days BETWEEN 1 AND 3650)
) ENGINE=InnoDB;

CREATE TABLE inventory_movements (
    movement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    batch_id BIGINT UNSIGNED NOT NULL,
    movement_type VARCHAR(20) NOT NULL,
    quantity_delta DECIMAL(12, 3) NOT NULL,
    source_type VARCHAR(20) NULL,
    source_id BIGINT UNSIGNED NULL,
    moved_at DATETIME NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    note VARCHAR(255) NULL,
    PRIMARY KEY (movement_id),
    KEY ix_movements_batch_date (batch_id, moved_at),
    KEY ix_movements_source (source_type, source_id),
    CONSTRAINT fk_movements_batch
        FOREIGN KEY (batch_id) REFERENCES inventory_batches (batch_id),
    CONSTRAINT fk_movements_user
        FOREIGN KEY (created_by) REFERENCES users (user_id),
    CONSTRAINT chk_movements_type
        CHECK (movement_type IN ('RECEIPT', 'SALE', 'ADJUSTMENT', 'DAMAGED', 'EXPIRED')),
    CONSTRAINT chk_movements_quantity CHECK (quantity_delta <> 0)
) ENGINE=InnoDB;

-- Du lieu gia lap cho demo va kiem thu. Ten, lien he, ma vach va gia chi la du lieu mau.
START TRANSACTION;
INSERT INTO roles (role_id, role_code, role_name, description) VALUES
    (1, 'ADMIN', 'Quan tri vien', 'Quan ly tai khoan va toan bo nghiep vu'),
    (2, 'STAFF', 'Nhan vien', 'Thuc hien nghiep vu duoc phan cong');

-- password_hash la gia tri mau, khong phai hash dang nhap hop le.
-- Hay thay bang bcrypt/Argon2 hash duoc tao boi ung dung truoc khi su dung that.
INSERT INTO users (user_id, role_id, username, password_hash, is_active) VALUES
    (1, 1, 'admin_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1),
    (2, 2, 'staff_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1),
    (1001, 2, 'staff_khang_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1),
    (1002, 2, 'staff_vy_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1),
    (1003, 2, 'staff_nam_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1),
    (1004, 2, 'staff_hanh_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 0);

INSERT INTO employees
    (employee_id, user_id, employee_code, full_name, phone, email, hire_date, employment_status)
VALUES
    (1, 2, 'NV0001', 'Nguyen Thi An', '0900000001', 'an.demo@example.com', '2025-01-15', 'ACTIVE'),
    (1001, 1001, 'NV1001', 'Trần Minh Khang', '0900000101', 'khang.demo@example.com', '2024-08-05', 'ACTIVE'),
    (1002, 1002, 'NV1002', 'Lê Thảo Vy', '0900000102', 'vy.demo@example.com', '2025-02-10', 'ACTIVE'),
    (1003, 1003, 'NV1003', 'Phạm Hoài Nam', '0900000103', 'nam.demo@example.com', '2025-05-12', 'ACTIVE'),
    (1004, 1004, 'NV1004', 'Võ Ngọc Hạnh', '0900000104', 'hanh.demo@example.com', '2025-08-18', 'ON_LEAVE');

INSERT INTO shifts (shift_id, shift_name, start_time, end_time, break_minutes, is_active) VALUES
    (1, 'Ca sang', '08:00:00', '12:00:00', 0, 1),
    (2, 'Ca chieu', '13:00:00', '17:00:00', 0, 1),
    (1001, 'Ca tối', '17:00:00', '21:00:00', 15, 1);

INSERT INTO employee_availabilities
    (availability_id, employee_id, available_date, start_time, end_time, note)
VALUES
    (1, 1, '2026-11-02', '08:00:00', '16:00:00', 'Dang ky ca cho tuan tiep theo');

INSERT INTO work_schedules (schedule_id, week_start_date, status, created_by) VALUES
    (1, '2026-11-02', 'PUBLISHED', 1),
    (1001, '2026-11-09', 'PUBLISHED', 1),
    (1002, '2026-11-16', 'PUBLISHED', 1),
    (1003, '2026-11-23', 'PUBLISHED', 1),
    (1004, '2026-11-30', 'DRAFT', 1);

INSERT INTO schedule_assignments
    (assignment_id, schedule_id, employee_id, shift_id, work_date, status, note)
VALUES
    (1, 1, 1, 1, '2026-11-02', 'COMPLETED', NULL),
    (1001, 1001, 1,    1,    '2026-11-09', 'COMPLETED', NULL),
    (1002, 1001, 1001, 2,    '2026-11-09', 'COMPLETED', NULL),
    (1003, 1001, 1003, 1001, '2026-11-10', 'COMPLETED', 'Bàn giao quầy cuối ngày'),
    (1004, 1001, 1002, 1,    '2026-11-12', 'COMPLETED', NULL),
    (1005, 1001, 1001, 2,    '2026-11-14', 'COMPLETED', 'Tăng cường cuối tuần'),
    (1006, 1001, 1003, 1001, '2026-11-14', 'COMPLETED', NULL),
    (1007, 1002, 1002, 1,    '2026-11-16', 'COMPLETED', NULL),
    (1008, 1002, 1,    2,    '2026-11-16', 'COMPLETED', NULL),
    (1009, 1002, 1004, 1001, '2026-11-17', 'COMPLETED', NULL),
    (1010, 1002, 1003, 1,    '2026-11-18', 'COMPLETED', NULL),
    (1011, 1002, 1004, 2,    '2026-11-19', 'CANCELLED', 'Đổi ca trước ngày nghỉ phép'),
    (1012, 1002, 1001, 2,    '2026-11-19', 'COMPLETED', 'Nhận ca thay'),
    (1013, 1002, 1002, 1,    '2026-11-21', 'PLANNED',   'Vắng mặt có báo trước'),
    (1014, 1002, 1,    1,    '2026-11-21', 'COMPLETED', 'Bổ sung nhân sự ca sáng'),
    (1015, 1002, 1003, 1001, '2026-11-21', 'COMPLETED', NULL),
    (1016, 1003, 1001, 1,    '2026-11-23', 'COMPLETED', NULL),
    (1017, 1003, 1002, 2,    '2026-11-23', 'COMPLETED', NULL),
    (1018, 1003, 1003, 1001, '2026-11-23', 'COMPLETED', NULL),
    (1019, 1003, 1,    1,    '2026-11-24', 'COMPLETED', NULL),
    (1020, 1003, 1001, 1001, '2026-11-24', 'PLANNED',   NULL),
    (1021, 1003, 1002, 1,    '2026-11-25', 'PLANNED',   NULL),
    (1022, 1003, 1003, 2,    '2026-11-26', 'PLANNED',   NULL),
    (1023, 1003, 1,    1,    '2026-11-28', 'PLANNED',   'Ca cuối tuần');

INSERT INTO employee_availabilities
    (availability_id, employee_id, available_date, start_time, end_time, note)
SELECT a.assignment_id, a.employee_id, a.work_date, s.start_time, s.end_time, 'Đăng ký tại đầu tuần'
FROM schedule_assignments a
JOIN shifts s ON s.shift_id = a.shift_id
WHERE a.assignment_id BETWEEN 1001 AND 1023 AND a.status <> 'CANCELLED';

INSERT INTO employee_availabilities
    (availability_id, employee_id, available_date, start_time, end_time, note)
VALUES
    (1024, 1001, '2026-11-30', '08:00:00', '12:00:00', 'Chờ xếp tuần mới'),
    (1025, 1002, '2026-11-30', '13:00:00', '17:00:00', 'Chờ xếp tuần mới'),
    (1026, 1003, '2026-12-01', '17:00:00', '21:00:00', 'Chờ xếp tuần mới');

INSERT INTO attendance_records (attendance_id, assignment_id, check_in_at, check_out_at, note) VALUES
    (1, 1, '2026-11-02 08:02:00', '2026-11-02 12:05:00', 'Nhan vien den muon 2 phut'),
    (1001, 1001, '2026-11-09 07:57:00', '2026-11-09 12:02:00', NULL),
    (1002, 1002, '2026-11-09 12:59:00', '2026-11-09 17:04:00', NULL),
    (1003, 1003, '2026-11-10 17:06:00', '2026-11-10 21:04:00', 'Đến muộn 6 phút'),
    (1004, 1004, '2026-11-12 07:59:00', '2026-11-12 12:01:00', NULL),
    (1005, 1005, '2026-11-14 12:55:00', '2026-11-14 17:06:00', NULL),
    (1006, 1006, '2026-11-14 16:59:00', '2026-11-14 21:03:00', NULL),
    (1007, 1007, '2026-11-16 07:54:00', '2026-11-16 12:04:00', NULL),
    (1008, 1008, '2026-11-16 13:09:00', '2026-11-16 17:09:00', 'Đến muộn 9 phút'),
    (1009, 1009, '2026-11-17 16:55:00', '2026-11-17 21:00:00', NULL),
    (1010, 1010, '2026-11-18 07:58:00', '2026-11-18 11:46:00', 'Về sớm đã xin phép'),
    (1012, 1012, '2026-11-19 12:57:00', '2026-11-19 17:08:00', 'Bàn giao sau ca'),
    (1013, 1013, NULL, NULL, 'Có báo nghỉ do việc gia đình'),
    (1014, 1014, '2026-11-21 07:55:00', '2026-11-21 12:05:00', NULL),
    (1015, 1015, '2026-11-21 16:57:00', '2026-11-21 21:06:00', NULL),
    (1016, 1016, '2026-11-23 07:59:00', '2026-11-23 12:03:00', NULL),
    (1017, 1017, '2026-11-23 13:03:00', '2026-11-23 17:05:00', 'Đến muộn 3 phút'),
    (1018, 1018, '2026-11-23 16:57:00', '2026-11-23 21:03:00', NULL),
    (1019, 1019, '2026-11-24 07:58:00', '2026-11-24 12:00:00', NULL);

INSERT INTO product_categories (category_id, category_name, description, is_active) VALUES
    (1, 'Thuoc khong ke don', 'Nhom san pham khong yeu cau don thuoc', 1),
    (2, 'Thuc pham bao ve suc khoe', 'Vitamin va san pham bo sung', 1),
    (1001, 'Thuoc ke don', 'Khong ban neu khong co toa duoc chu y', 1),
    (1002, 'Thuoc ho tro dieu tri', 'Thuoc bo sung hoac ho tro dieu tri', 1),
    (1003, 'Duong da va cham soc ca nhan', 'Kem, nuoc rua va dich cham soc', 1),
    (1004, 'Dung cu y te', 'Thiet bi do va cham soc tai nha', 1),
    (1005, 'San pham me va be', 'San pham cham soc suc khoe cho me va be', 1);

INSERT INTO suppliers (supplier_id, supplier_code, supplier_name, phone, email, address, is_active) VALUES
    (1, 'NCC001', 'Cong ty Duoc Minh Tam', '02830000001', 'sales@minhtam.example', 'TP Ho Chi Minh', 1),
    (2, 'NCC002', 'Nha phan phoi An Khang', '02830000002', 'contact@ankhang.example', 'TP Ho Chi Minh', 1),
    (1001, 'NCC1001', 'Cong ty CP Duoc An Phat', '02873110001', 'sales@anphat.example', 'Binh Duong', 1),
    (1002, 'NCC1002', 'Cong ty TNHH Y te Phuong Dong', '02923881100', 'order@phuongdong.example', 'Can Tho', 1),
    (1003, 'NCC1003', 'Nha phan phoi Suc Khoe Viet', '02439771122', 'info@suckhoeviet.example', 'Ha Noi', 1),
    (1004, 'NCC1004', 'Cong ty Duoc Hai Au', '02871002233', 'haiau.sales@example.com', 'Dong Nai', 1),
    (1005, 'NCC1005', 'Tong kho Duoc Kieu Ninh', '02923884444', 'kieuninh@example.com', 'Can Tho', 1);

INSERT INTO products
    (product_id, category_id, product_code, barcode, product_name, dosage_form, unit_name,
     requires_prescription, is_active)
VALUES
    (1, 1, 'SP0001', '8930000000001', 'Paracetamol 500 mg', 'Vien nen', 'vi', 0, 1),
    (2, 2, 'SP0002', '8930000000002', 'Vitamin C 500 mg', 'Vien nen', 'hop', 0, 1),
    (1001, 1001, 'SP1001', '8938500010011', 'Amoxicillin 500 mg', 'Vien nang', 'hop', 1, 1),
    (1002, 1001, 'SP1002', '8938500010028', 'Cetirizine 10 mg', 'Vien nen', 'hop', 0, 1),
    (1003, 2, 'SP1003', '8938500010035', 'Omega-3 Fish Oil', 'Vien nang mem', 'hop', 0, 1),
    (1004, 1002, 'SP1004', '8938500010042', 'Siro ho Prospan', 'Dung dich', 'chai', 0, 1),
    (1005, 1003, 'SP1005', '8938500010059', 'Gel rua tay kho', 'Gel', 'chai', 0, 1),
    (1006, 1004, 'SP1006', '8938500010066', 'May do huyet ap bap tay', 'Thiet bi y te', 'bo', 0, 1),
    (1007, 1005, 'SP1007', '8938500010073', 'Khau trang tre em', 'Van may', 'hop', 0, 1),
    (1008, 1002, 'SP1008', '8938500010080', 'Dung dich suc mieng Chlorhexidine', 'Dung dich', 'chai', 0, 1);

UPDATE products
SET unit_price = CASE product_id
        WHEN 1 THEN 1200.00
        WHEN 2 THEN 38000.00
        ELSE unit_price
    END,
    selling_price = CASE product_id
        WHEN 1 THEN 2500.00
        WHEN 2 THEN 55000.00
        WHEN 1001 THEN 85000.00
        WHEN 1002 THEN 32000.00
        WHEN 1003 THEN 145000.00
        WHEN 1004 THEN 78000.00
        WHEN 1005 THEN 45000.00
        WHEN 1006 THEN 520000.00
        WHEN 1007 THEN 68000.00
        WHEN 1008 THEN 42000.00
        ELSE 10000.00
    END
WHERE product_id BETWEEN 1 AND 1008;

INSERT INTO supplier_products (supplier_id, product_id, supplier_sku, purchase_price, lead_time_days) VALUES
    (1, 1, 'MT-PARA-500', 1000.00, 2),
    (2, 2, 'AK-VITC-500', 35000.00, 3),
    (1001, 1001, 'AP-AMOX-500', 62000.00, 4),
    (1001, 1002, 'AP-CETI-10', 24000.00, 3),
    (1002, 1003, 'PD-OMEGA-3', 98000.00, 5),
    (1003, 1004, 'SKV-PROSPAN', 57000.00, 4),
    (1003, 1005, 'SKV-GEL-01', 28000.00, 2),
    (1004, 1006, 'HA-BPM-01', 410000.00, 7),
    (1005, 1007, 'KN-MASK-KID', 39000.00, 2),
    (1005, 1008, 'KN-CHX-01', 30000.00, 3);

INSERT INTO product_ingredients (product_id, ingredient_name) VALUES
    (1, 'Paracetamol'),
    (2, 'Vitamin C'),
    (1001, 'Amoxicillin'),
    (1002, 'Cetirizine'),
    (1003, 'Omega-3'),
    (1003, 'EPA'),
    (1003, 'DHA'),
    (1004, 'Extractum Thymi'),
    (1004, 'Hedera Helix'),
    (1005, 'Ethanol'),
    (1008, 'Chlorhexidine');

INSERT INTO warehouse_locations (location_id, location_code, location_name, description, is_active) VALUES
    (1, 'KE-A1', 'Ke A tang 1', 'Khu trung bay thuoc khong ke don', 1),
    (2, 'KE-B1', 'Ke B tang 1', 'Khu trung bay thuc pham bao ve suc khoe', 1),
    (1001, 'KE-C1', 'Ke thuoc ke don', 'Ke luu thuoc can toa bac si', 1),
    (1002, 'TU-LANH-01', 'Tu lanh sinh ly', 'Bao quan san pham can dieu kien lanh', 1),
    (1003, 'KE-D1', 'Ke dung cu y te', 'May do va dung cu cham soc tai nha', 1),
    (1004, 'KE-E1', 'Ke me va be', 'San pham cham soc cho tre em', 1);

INSERT INTO inventory_batches
    (batch_id, product_id, location_id, batch_code, expiry_date, quantity_on_hand)
VALUES
    (1, 1, 1, 'LOT-PARA-001', '2027-12-31', 97.000),
    (2, 2, 2, 'LOT-VITC-001', '2028-01-31', 50.000),
    (1001, 1001, 1001, 'LOT-AMOX-2601', '2027-06-30', 65.000),
    (1002, 1002, 1, 'LOT-CETI-2602', '2027-09-30', 80.000),
    (1003, 1003, 2, 'LOT-OMEGA-2603', '2027-11-30', 45.000),
    (1004, 1004, 2, 'LOT-PROSPAN-2604', '2027-04-30', 35.000),
    (1005, 1005, 1, 'LOT-GEL-2605', '2028-02-29', 120.000),
    (1006, 1006, 1003, 'LOT-BPM-2606', '2029-01-31', 18.000),
    (1007, 1007, 1004, 'LOT-MASK-KID-2607', '2028-06-30', 75.000),
    (1008, 1008, 1, 'LOT-CHX-2608', '2027-08-31', 40.000);

INSERT INTO goods_receipts
    (receipt_id, receipt_code, supplier_id, received_by, received_at, status, note)
VALUES
    (1, 'PN0001', 1, 2, '2026-10-27 09:00:00', 'RECEIVED', 'Phieu nhap mau');

INSERT INTO goods_receipt_items (receipt_item_id, receipt_id, batch_id, quantity, unit_cost) VALUES
    (1, 1, 1, 100.000, 1000.00);

INSERT INTO stocktakes (stocktake_id, location_id, stocktake_date, status, created_by, note) VALUES
    (1, 1, '2026-10-27', 'COMPLETED', 1, 'Kiem ke mau cuoi ngay');

INSERT INTO stocktake_items
    (stocktake_item_id, stocktake_id, batch_id, expected_quantity, counted_quantity, difference_quantity)
VALUES
    (1, 1, 1, 98.000, 97.000, -1.000);

INSERT INTO customers
    (customer_id, customer_code, full_name, phone, email, date_of_birth,
     loyalty_points, is_active, is_blocked, block_reason)
VALUES
    (1, 'KH0001', 'Tran Minh Binh', '0910000001', 'binh.demo@example.com', '1996-03-18', 4, 1, 0, NULL),
    (1001, 'KH1001', 'Nguyen Hoang Lan', '0912345001', 'lan.nguyen@example.com', '1990-07-22', 1250, 1, 0, NULL),
    (1002, 'KH1002', 'Le Thi Mai', '0912345002', 'mai.le@example.com', '1988-11-05', 680, 1, 0, NULL),
    (1003, 'KH1003', 'Pham Duc Long', '0912345003', 'long.pham@example.com', '1975-02-14', 220, 1, 0, NULL),
    (1004, 'KH1004', 'Vo Minh Chau', '0912345004', 'chau.vo@example.com', '2001-09-30', 90, 1, 0, NULL),
    (1005, 'KH1005', 'Dang Quoc Bao', '0912345005', 'bao.dang@example.com', '1982-04-12', 40, 1, 1, 'Yeu cau xac minh truoc moi lan mua thuoc ke don'),
    (1006, 'KH1006', 'Huynh Anh Thu', '0912345006', 'thu.huynh@example.com', '1998-12-25', 175, 1, 0, NULL),
    (1007, 'KH1007', 'Bui Gia Huy', '0912345007', 'huy.bui@example.com', '1965-06-08', 520, 1, 0, NULL),
    (1008, 'KH1008', 'Nguyen Kim Oanh', '0912345008', 'oanh.nguyen@example.com', '1993-01-19', 35, 0, 0, NULL),
    (1009, 'KH1009', 'Tran Van Khoi', '0912345009', 'khoi.tran@example.com', '1980-10-03', 310, 1, 0, NULL);

INSERT INTO customer_allergies (customer_allergy_id, customer_id, allergen_name, note) VALUES
    (1, 1, 'Penicillin', 'Khach hang khai bao di ung'),
    (1001, 1001, 'Paracetamol', 'Da tung co phan ung khi dung lieu cao'),
    (1002, 1002, 'Iodine', 'Di ung phan ung qua da'),
    (1003, 1003, 'Aspirin', 'Can tranh cac san pham co Aspirin'),
    (1004, 1006, 'Chlorhexidine', 'Phat ban nhe sau khi dung dich suc mieng'),
    (1005, 1007, 'Penicillin', 'Dien tich man di trong ho so');

INSERT INTO loyalty_programs
    (loyalty_program_id, program_name, points_per_1000_currency, currency_per_point, is_active)
VALUES
    (1, 'Tich diem co ban', 1.0000, 1000.00, 1),
    (1001, 'Tich diem thanh vien than thiet', 2.0000, 800.00, 1);

INSERT INTO promotions
    (promotion_id, promotion_code, promotion_name, discount_percent, points_multiplier,
     starts_at, points_redemption_starts_at, points_earning_ends_at, ends_at,
     redemption_vnd_per_point, is_active)
VALUES
    (1, 'WELCOME10', 'Uu dai khach hang moi', 10.00, 1.50,
     '2026-10-01 00:00:00', '2026-10-01 00:00:00',
     '2026-12-31 23:59:59', '2026-12-31 23:59:59', 1000.00, 1),
    (1001, 'FAMILY15', 'Chuong trinh gia dinh', 15.00, 1.80,
     '2026-11-01 00:00:00', '2026-11-01 00:00:00',
     '2026-12-31 23:59:59', '2026-12-31 23:59:59', 900.00, 1),
    (1002, 'VIP20', 'Uu dai khach hang than thiet', 20.00, 2.00,
     '2026-09-15 00:00:00', '2026-09-20 00:00:00',
     '2026-12-15 23:59:59', '2026-12-31 23:59:59', 800.00, 1),
    (1003, 'HEALTH5', 'Chuong trinh thang suc khoe', 5.00, 1.20,
     '2026-11-01 00:00:00', '2026-11-01 00:00:00',
     '2026-11-30 23:59:59', '2026-11-30 23:59:59', 1000.00, 1),
    (1004, 'KIDS12', 'Uu dai san pham me va be', 12.00, 1.50,
     '2026-11-05 00:00:00', '2026-11-05 00:00:00',
     '2027-01-15 23:59:59', '2027-01-31 23:59:59', 950.00, 1),
    (1005, 'BLACKFRIDAY', 'Khuyen mai cuoi nam', 18.00, 2.20,
     '2026-11-20 00:00:00', '2026-11-21 00:00:00',
     '2026-11-30 23:59:59', '2026-12-03 23:59:59', 850.00, 0),
    (1006, 'POINTONLY', 'Chuong trinh dung diem', 0.00, 1.00,
     '2026-01-01 00:00:00', '2026-01-01 00:00:00',
     '2027-12-31 23:59:59', '2027-12-31 23:59:59', 1000.00, 1);

INSERT INTO promotion_point_tiers
    (promotion_id, minimum_invoice_total_vnd, points_rate_percent)
VALUES
    (1, 0, 0.15),
    (1, 500000, 0.25),
    (1001, 0, 0.18),
    (1001, 300000, 0.30),
    (1002, 0, 0.20),
    (1002, 1000000, 0.40),
    (1003, 0, 0.12),
    (1004, 0, 0.15),
    (1005, 0, 0.22),
    (1006, 0, 0.10);

INSERT INTO sales
    (sale_id, sale_code, customer_id, employee_id, promotion_id, sold_at, subtotal,
     discount_amount, total_amount, points_earned, points_redeemed, status)
VALUES
    (1, 'HD0001', 1, 1, 1, '2026-10-27 10:00:00', 5000.00, 500.00, 4500.00, 4, 0, 'COMPLETED'),
    (1001, 'HD1001', 1001, 1, 1001, '2026-11-02 09:15:00', 145000.00, 21750.00, 123250.00, 120, 0, 'COMPLETED'),
    (1002, 'HD1002', 1002, 1001, 1003, '2026-11-03 10:40:00', 32000.00, 1600.00, 30400.00, 36, 0, 'COMPLETED'),
    (1003, 'HD1003', 1003, 1002, 1, '2026-11-05 14:20:00', 170000.00, 17000.00, 153000.00, 153, 0, 'COMPLETED'),
    (1004, 'HD1004', 1004, 1003, 1004, '2026-11-08 16:35:00', 68000.00, 8160.00, 59840.00, 89, 0, 'COMPLETED'),
    (1005, 'HD1005', 1006, 1, 1006, '2026-11-10 08:50:00', 42000.00, 0.00, 42000.00, 42, 0, 'COMPLETED'),
    (1006, 'HD1006', 1007, 1001, 1002, '2026-11-12 11:05:00', 520000.00, 104000.00, 416000.00, 832, 0, 'COMPLETED'),
    (1007, 'HD1007', 1009, 1002, 1, '2026-11-15 15:30:00', 85500.00, 8550.00, 76950.00, 76, 0, 'COMPLETED'),
    (1008, 'HD1008', 1001, 1003, 1005, '2026-11-20 09:45:00', 225000.00, 40500.00, 184500.00, 369, 0, 'COMPLETED'),
    (1009, 'HD1009', 1002, 1, 1003, '2026-11-21 13:25:00', 78000.00, 3900.00, 74100.00, 74, 0, 'COMPLETED'),
    (1010, 'HD1010', 1006, 1001, 1004, '2026-11-23 17:10:00', 136000.00, 16320.00, 119680.00, 119, 0, 'COMPLETED'),
    (1011, 'HD1011', 1003, 1002, NULL, '2026-11-24 10:15:00', 85000.00, 0.00, 85000.00, 0, 0, 'PENDING'),
    (1012, 'HD1012', 1004, 1003, NULL, '2026-11-24 11:20:00', 55000.00, 0.00, 55000.00, 0, 0, 'VOIDED'),
    (1013, 'HD1013', 1001, 1, 1001, '2026-11-24 14:20:00', 260000.00, 39000.00, 221000.00, 442, 0, 'COMPLETED'),
    (1014, 'HD1014', 1002, 1001, 1002, '2026-11-24 18:25:00', 410000.00, 82000.00, 328000.00, 656, 0, 'COMPLETED');

INSERT INTO sale_items
    (sale_item_id, sale_id, product_id, quantity, unit_price, line_discount, line_total)
VALUES
    (1, 1, 1, 2.000, 2500.00, 500.00, 4500.00),
    (1001, 1001, 1003, 1.000, 145000.00, 21750.00, 123250.00),
    (1002, 1002, 1002, 1.000, 32000.00, 1600.00, 30400.00),
    (1003, 1003, 1001, 2.000, 85000.00, 17000.00, 153000.00),
    (1004, 1004, 1007, 1.000, 68000.00, 8160.00, 59840.00),
    (1005, 1005, 1008, 1.000, 42000.00, 0.00, 42000.00),
    (1006, 1006, 1006, 1.000, 520000.00, 104000.00, 416000.00),
    (1007, 1007, 1001, 1.000, 85500.00, 8550.00, 76950.00),
    (1008, 1008, 1003, 1.000, 225000.00, 40500.00, 184500.00),
    (1009, 1009, 1004, 1.000, 78000.00, 3900.00, 74100.00),
    (1010, 1010, 1005, 2.000, 68000.00, 16320.00, 119680.00),
    (1011, 1011, 1001, 1.000, 85000.00, 0.00, 85000.00),
    (1012, 1012, 2, 1.000, 55000.00, 0.00, 55000.00),
    (1013, 1013, 1003, 1.000, 260000.00, 39000.00, 221000.00),
    (1014, 1014, 1006, 1.000, 410000.00, 82000.00, 328000.00);

INSERT INTO inventory_movements
    (movement_id, batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
VALUES
    (1, 1, 'RECEIPT', 100.000, 'GOODS_RECEIPT', 1, '2026-10-27 09:00:00', 2, 'Nhap kho theo PN0001'),
    (2, 1, 'SALE', -2.000, 'SALE', 1, '2026-10-27 10:00:00', 2, 'Ban hang theo HD0001'),
    (3, 1, 'ADJUSTMENT', -1.000, 'STOCKTAKE', 1, '2026-10-27 17:00:00', 1, 'Dieu chinh sau kiem ke'),
    (1001, 1003, 'SALE', -1.000, 'SALE', 1001, '2026-11-02 09:15:00', 2, 'Ban Omega-3 theo HD1001'),
    (1002, 1002, 'SALE', -1.000, 'SALE', 1002, '2026-11-03 10:40:00', 1001, 'Ban Cetirizine theo HD1002'),
    (1003, 1001, 'SALE', -2.000, 'SALE', 1003, '2026-11-05 14:20:00', 1002, 'Ban Amoxicillin theo HD1003'),
    (1004, 1007, 'SALE', -1.000, 'SALE', 1004, '2026-11-08 16:35:00', 1003, 'Ban khau trang tre em theo HD1004'),
    (1005, 1008, 'SALE', -1.000, 'SALE', 1005, '2026-11-10 08:50:00', 2, 'Ban nuoc suc mieng theo HD1005'),
    (1006, 1006, 'SALE', -1.000, 'SALE', 1006, '2026-11-12 11:05:00', 1001, 'Ban may do huyet ap theo HD1006'),
    (1007, 1001, 'SALE', -1.000, 'SALE', 1007, '2026-11-15 15:30:00', 1002, 'Ban Amoxicillin theo HD1007'),
    (1008, 1003, 'SALE', -1.000, 'SALE', 1008, '2026-11-20 09:45:00', 1003, 'Ban Omega-3 theo HD1008'),
    (1009, 1004, 'SALE', -1.000, 'SALE', 1009, '2026-11-21 13:25:00', 2, 'Ban siro ho theo HD1009'),
    (1010, 1005, 'SALE', -2.000, 'SALE', 1010, '2026-11-23 17:10:00', 1001, 'Ban gel rua tay theo HD1010'),
    (1011, 1003, 'SALE', -1.000, 'SALE', 1013, '2026-11-24 14:20:00', 2, 'Ban Omega-3 theo HD1013'),
    (1012, 1006, 'SALE', -1.000, 'SALE', 1014, '2026-11-24 18:25:00', 1001, 'Ban may do huyet ap theo HD1014');

-- Bo sung thong tin toa va tach giam gia chuong trinh cho giao dich mau.
UPDATE sales
SET promotion_discount_amount = discount_amount
WHERE promotion_id IS NOT NULL;

UPDATE sales sale
SET sale.points_earned = FLOOR(sale.total_amount * (
    SELECT tier.points_rate_percent
    FROM promotion_point_tiers tier
    WHERE tier.promotion_id = sale.promotion_id
      AND tier.minimum_invoice_total_vnd <= sale.total_amount
    ORDER BY tier.minimum_invoice_total_vnd DESC
    LIMIT 1
) / 100)
WHERE sale.sale_id BETWEEN 1001 AND 1014
  AND sale.status = 'COMPLETED';

UPDATE sales
SET prescription_code = CONCAT('TOA-MAU-', sale_id), prescription_date = DATE(sold_at),
    prescribing_doctor = 'Bác sĩ mẫu', clinic = 'Phòng khám giả lập'
WHERE sale_id IN (1003, 1007, 1011);

INSERT INTO customers (customer_id, customer_code, full_name, phone, email, date_of_birth)
VALUES (1010, 'KH1010', 'Phan Bảo Ngọc', '0912345010', 'ngoc.phan@example.com', '1999-05-22');

INSERT INTO customer_allergies (customer_id, allergen_name, note)
VALUES (1007, 'Amoxicillin', 'Thông tin giả lập để kiểm thử cảnh báo khi chọn sản phẩm SP1001');

-- Bien dong theo lo: ton gan het han, het han da xu ly, phieu nhap va kiem ke.
INSERT INTO inventory_batches (batch_id, product_id, location_id, batch_code, expiry_date, quantity_on_hand)
VALUES
    (1009, 1002, 1, 'LOT-CETI-DEMO-GAN', DATE_ADD(CURRENT_DATE(), INTERVAL 7 DAY), 0),
    (1010, 1002, 1, 'LOT-CETI-DEMO-XA', DATE_ADD(CURRENT_DATE(), INTERVAL 300 DAY), 0),
    (1011, 1004, 2, 'LOT-HO-DEMO-HET', DATE_SUB(CURRENT_DATE(), INTERVAL 3 DAY), 0);

INSERT INTO goods_receipts (receipt_id, receipt_code, supplier_id, received_by, received_at, status, note)
VALUES
    (1001, 'PN1001', 2, 1001, '2025-12-01 08:20:00', 'RECEIVED', 'Nhập lô vitamin'),
    (1002, 'PN1002', 1001, 1002, '2025-12-01 09:10:00', 'RECEIVED', 'Nhập lô thuốc và hàng bán quầy'),
    (1003, 'PN1003', 1002, 1001, '2025-12-02 10:05:00', 'RECEIVED', 'Nhập lô thực phẩm bổ sung'),
    (1004, 'PN1004', 1003, 1003, '2025-12-03 14:15:00', 'RECEIVED', 'Nhập lô chăm sóc cá nhân'),
    (1005, 'PN1005', 1004, 1002, '2025-12-04 15:03:00', 'RECEIVED', 'Nhập hàng thiết bị y tế'),
    (1006, 'PN1006', 1005, 1001, '2025-12-05 09:35:00', 'RECEIVED', 'Nhập khẩu trang và dung dịch'),
    (1007, 'PN1007', 1001, 1003, DATE_SUB(NOW(), INTERVAL 6 DAY), 'RECEIVED', 'Hai lô cùng mặt hàng để thử FEFO'),
    (1008, 'PN1008', 1003, 1003, DATE_SUB(NOW(), INTERVAL 40 DAY), 'RECEIVED', 'Lô mẫu phục vụ báo cáo hết hạn'),
    (1009, 'PN1009', 1004, 1002, DATE_SUB(NOW(), INTERVAL 2 DAY), 'CANCELLED', 'Hủy vì giao sai quy cách');

INSERT INTO goods_receipt_items (receipt_id, batch_id, quantity, unit_cost)
VALUES
    (1001, 2, 50, 35000.00),
    (1002, 1001, 80, 62000.00), (1002, 1002, 100, 24000.00),
    (1003, 1003, 65, 98000.00),
    (1004, 1004, 55, 57000.00), (1004, 1005, 155, 28000.00),
    (1005, 1006, 24, 410000.00),
    (1006, 1007, 90, 39000.00), (1006, 1008, 55, 30000.00),
    (1007, 1009, 8, 23000.00), (1007, 1010, 40, 24000.00),
    (1008, 1011, 6, 56000.00);

INSERT INTO inventory_movements
    (batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
SELECT item.batch_id, 'RECEIPT', item.quantity, 'GOODS_RECEIPT', receipt.receipt_id,
       receipt.received_at, receipt.received_by, CONCAT('Nhập lô theo ', receipt.receipt_code)
FROM goods_receipt_items item
JOIN goods_receipts receipt ON receipt.receipt_id = item.receipt_id
WHERE receipt.receipt_id >= 1001;

INSERT INTO inventory_movements
    (batch_id, movement_type, quantity_delta, source_type, moved_at, created_by, note)
VALUES
    (1005, 'DAMAGED', -3, 'INVENTORY_LOSS', DATE_SUB(NOW(), INTERVAL 8 DAY), 1001, 'Ba chai bị nứt bao bì'),
    (1011, 'EXPIRED', -6, 'INVENTORY_LOSS', DATE_SUB(NOW(), INTERVAL 2 DAY), 1002, 'Đã loại sáu chai hết hạn');

-- Lich su thanh toan qua nhieu thang, quy va hai nam; luu gia tai thoi diem ban.
INSERT INTO sales
    (sale_id, sale_code, customer_id, employee_id, promotion_id, sold_at, subtotal,
     promotion_discount_amount, points_discount_amount, discount_amount, total_amount,
     points_earned, points_redeemed, redeem_points, requested_points)
VALUES
    (2001, 'HD2001', 1001, 1, NULL, '2025-12-20 09:12:00', 235000, 0, 0, 0, 235000, 235, 0, FALSE, 0),
    (2002, 'HD2002', 1002, 1001, NULL, '2025-12-27 14:34:00', 110000, 0, 0, 0, 110000, 110, 0, FALSE, 0),
    (2003, 'HD2003', 1003, 1002, 1006, '2026-01-09 08:25:00', 178000, 0, 0, 0, 178000, 178, 0, FALSE, 0),
    (2004, 'HD2004', 1006, 1, 1006, '2026-01-26 11:10:00', 290000, 0, 0, 0, 290000, 290, 0, FALSE, 0),
    (2005, 'HD2005', 1007, 1003, 1006, '2026-02-15 16:40:00', 565000, 0, 0, 0, 565000, 565, 0, FALSE, 0),
    (2006, 'HD2006', 1001, 1002, 1006, '2026-02-27 17:25:00', 223000, 0, 12000, 12000, 211000, 211, 12, TRUE, 12),
    (2007, 'HD2007', 1002, 1, 1006, '2026-03-11 13:45:00', 152000, 0, 10000, 10000, 142000, 142, 10, TRUE, 10),
    (2008, 'HD2008', 1009, 1001, 1006, '2026-03-28 10:30:00', 141000, 0, 0, 0, 141000, 141, 0, FALSE, 0),
    (2009, 'HD2009', 1008, 1003, 1006, '2026-04-12 08:35:00', 100000, 0, 0, 0, 100000, 100, 0, FALSE, 0),
    (2010, 'HD2010', 1001, 1001, 1006, '2026-04-28 15:25:00', 368000, 0, 15000, 15000, 353000, 353, 15, TRUE, 15),
    (2011, 'HD2011', 1004, 1002, 1006, '2026-05-14 11:20:00', 158000, 0, 0, 0, 158000, 158, 0, FALSE, 0),
    (2012, 'HD2012', 1006, 1003, 1006, '2026-05-30 18:25:00', 209000, 0, 14000, 14000, 195000, 195, 14, TRUE, 14),
    (2013, 'HD2013', 1007, 1, 1006, '2026-06-13 09:47:00', 520000, 0, 20000, 20000, 500000, 500, 20, TRUE, 20),
    (2014, 'HD2014', 1002, 1002, 1006, '2026-06-29 16:15:00', 201000, 0, 0, 0, 201000, 201, 0, FALSE, 0),
    (2015, 'HD2015', 1003, 1001, 1006, '2026-07-12 10:40:00', 181000, 0, 12000, 12000, 169000, 169, 12, TRUE, 12),
    (2016, 'HD2016', 1009, 1003, 1006, '2026-07-27 13:23:00', 162000, 0, 10000, 10000, 152000, 152, 10, TRUE, 10),
    (2017, 'HD2017', 1004, 1002, 1006, '2026-08-07 10:15:00', 235000, 0, 8000, 8000, 227000, 227, 8, TRUE, 8),
    (2018, 'HD2018', 1001, 1001, 1006, '2026-08-23 17:32:00', 200000, 0, 18000, 18000, 182000, 182, 18, TRUE, 18),
    (2019, 'HD2019', 1002, 1003, 1006, '2026-09-05 14:00:00', 223000, 0, 0, 0, 223000, 223, 0, FALSE, 0),
    (2020, 'HD2020', 1006, 1, 1002, '2026-09-24 08:58:00', 181000, 36200, 12000, 48200, 132800, 265, 15, TRUE, 15),
    (2021, 'HD2021', 1007, 1002, 1002, '2026-10-08 09:13:00', 562000, 112400, 20000, 132400, 429600, 859, 25, TRUE, 25),
    (2022, 'HD2022', 1009, 1001, 1002, '2026-10-19 16:22:00', 368000, 73600, 13600, 87200, 280800, 561, 17, TRUE, 17);

INSERT INTO sale_items (sale_item_id, sale_id, product_id, quantity, unit_price, line_total)
VALUES
    (2001, 2001, 1003, 1, 145000, 145000),
    (2002, 2001, 1005, 2, 45000, 90000),
    (2003, 2002, 1004, 1, 78000, 78000),
    (2004, 2002, 1002, 1, 32000, 32000),
    (2005, 2003, 1007, 2, 68000, 136000),
    (2006, 2003, 1008, 1, 42000, 42000),
    (2007, 2004, 1003, 2, 145000, 290000),
    (2008, 2005, 1006, 1, 520000, 520000),
    (2009, 2005, 1005, 1, 45000, 45000),
    (2010, 2006, 1003, 1, 145000, 145000),
    (2011, 2006, 1004, 1, 78000, 78000),
    (2012, 2007, 1007, 1, 68000, 68000),
    (2013, 2007, 1008, 2, 42000, 84000),
    (2014, 2008, 1002, 3, 32000, 96000),
    (2015, 2008, 1005, 1, 45000, 45000),
    (2016, 2009, 1007, 1, 68000, 68000),
    (2017, 2009, 1002, 1, 32000, 32000),
    (2018, 2010, 1003, 2, 145000, 290000),
    (2019, 2010, 1004, 1, 78000, 78000),
    (2020, 2011, 1005, 2, 45000, 90000),
    (2021, 2011, 1007, 1, 68000, 68000),
    (2022, 2012, 1002, 2, 32000, 64000),
    (2023, 2012, 1003, 1, 145000, 145000),
    (2024, 2013, 1006, 1, 520000, 520000),
    (2025, 2014, 1004, 2, 78000, 156000),
    (2026, 2014, 1005, 1, 45000, 45000),
    (2027, 2015, 1007, 2, 68000, 136000),
    (2028, 2015, 1005, 1, 45000, 45000),
    (2029, 2016, 1004, 1, 78000, 78000),
    (2030, 2016, 1008, 2, 42000, 84000),
    (2031, 2017, 1003, 1, 145000, 145000),
    (2032, 2017, 1005, 2, 45000, 90000),
    (2033, 2018, 1002, 2, 32000, 64000),
    (2034, 2018, 1007, 2, 68000, 136000),
    (2035, 2019, 1004, 1, 78000, 78000),
    (2036, 2019, 1003, 1, 145000, 145000),
    (2037, 2020, 1005, 1, 45000, 45000),
    (2038, 2020, 1007, 2, 68000, 136000),
    (2039, 2021, 1006, 1, 520000, 520000),
    (2040, 2021, 1008, 1, 42000, 42000),
    (2041, 2022, 1004, 1, 78000, 78000),
    (2042, 2022, 1003, 2, 145000, 290000);

INSERT INTO inventory_movements
    (batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
SELECT batch.batch_id, 'SALE', -item.quantity, 'SALE', sale.sale_id, sale.sold_at,
       employee.user_id, CONCAT('Xuất hàng theo ', sale.sale_code)
FROM sales sale
JOIN sale_items item ON item.sale_id = sale.sale_id
JOIN inventory_batches batch ON batch.batch_id = item.product_id
JOIN employees employee ON employee.employee_id = sale.employee_id
WHERE sale.sale_id BETWEEN 2001 AND 2022;

-- Bo sung 100 hoa don hoan tat tu thang 08/2024 den thang 11/2026 cho bao cao UC-19.
INSERT INTO promotions
    (promotion_id, promotion_code, promotion_name, discount_percent, starts_at,
     points_redemption_starts_at, points_earning_ends_at, ends_at, redemption_vnd_per_point)
VALUES
    (3001, 'MAU-CUOINAM-2024', 'Tri ân cuối năm 2024', 7.00, '2024-11-01 00:00:00',
     '2024-11-01 00:00:00', '2024-12-31 23:59:59', '2024-12-31 23:59:59', 1000.00),
    (3002, 'MAU-TET-2025', 'Ưu đãi đầu năm 2025', 10.00, '2025-01-01 00:00:00',
     '2025-01-01 00:00:00', '2025-02-20 23:59:59', '2025-02-20 23:59:59', 1000.00),
    (3003, 'MAU-HE-2025', 'Chăm sóc mùa hè 2025', 8.00, '2025-06-01 00:00:00',
     '2025-06-01 00:00:00', '2025-08-31 23:59:59', '2025-08-31 23:59:59', 1000.00);

INSERT INTO promotion_point_tiers (promotion_id, minimum_invoice_total_vnd, points_rate_percent)
VALUES (3001, 0, 0.12), (3002, 0, 0.15), (3003, 0, 0.13);

INSERT INTO sales
    (sale_id, sale_code, customer_id, employee_id, promotion_id, sold_at, subtotal,
     promotion_discount_amount, points_discount_amount, discount_amount, total_amount,
     points_earned, points_redeemed, redeem_points, requested_points)
VALUES
    (3001, 'HD3001', 1001, 1001, NULL, '2024-08-06 08:07:00', 131000, 0, 0, 0, 131000, 131, 0, FALSE, 0),
    (3002, 'HD3002', 1002, 1001, NULL, '2024-08-14 15:20:00', 526000, 0, 0, 0, 526000, 526, 0, FALSE, 0),
    (3003, 'HD3003', 1003, 1001, NULL, '2024-08-18 10:33:00', 128000, 0, 0, 0, 128000, 128, 0, FALSE, 0),
    (3004, 'HD3004', 1004, 1001, NULL, '2024-08-25 17:46:00', 164000, 0, 0, 0, 164000, 164, 0, FALSE, 0),
    (3005, 'HD3005', 1006, 1001, NULL, '2024-09-06 12:59:00', 99000, 0, 0, 0, 99000, 99, 0, FALSE, 0),
    (3006, 'HD3006', 1007, 1001, NULL, '2024-09-14 19:12:00', 199000, 0, 0, 0, 199000, 199, 0, FALSE, 0),
    (3007, 'HD3007', 1009, 1001, NULL, '2024-09-18 14:25:00', 152000, 0, 0, 0, 152000, 152, 0, FALSE, 0),
    (3008, 'HD3008', 1001, 1001, NULL, '2024-09-25 09:38:00', 192000, 0, 0, 0, 192000, 192, 0, FALSE, 0),
    (3009, 'HD3009', 1002, 1001, NULL, '2024-10-06 16:51:00', 184000, 0, 0, 0, 184000, 184, 0, FALSE, 0),
    (3010, 'HD3010', 1003, 1001, NULL, '2024-10-14 11:04:00', 280000, 0, 0, 0, 280000, 280, 0, FALSE, 0),
    (3011, 'HD3011', 1004, 1001, NULL, '2024-10-18 18:17:00', 192000, 0, 0, 0, 192000, 192, 0, FALSE, 0),
    (3012, 'HD3012', 1006, 1001, NULL, '2024-10-25 13:30:00', 730000, 0, 0, 0, 730000, 730, 0, FALSE, 0),
    (3013, 'HD3013', 1007, 1001, 3001, '2024-11-06 08:43:00', 244000, 17080, 0, 17080, 226920, 272, 0, FALSE, 0),
    (3014, 'HD3014', 1009, 1001, 3001, '2024-11-14 15:56:00', 599000, 41930, 0, 41930, 557070, 668, 0, FALSE, 0),
    (3015, 'HD3015', 1001, 1001, 3001, '2024-11-18 10:09:00', 497000, 34790, 0, 34790, 462210, 554, 0, FALSE, 0),
    (3016, 'HD3016', 1002, 1001, 3001, '2024-11-25 17:22:00', 87000, 6090, 0, 6090, 80910, 97, 0, FALSE, 0),
    (3017, 'HD3017', 1003, 1001, 3001, '2024-12-06 12:35:00', 497000, 34790, 0, 34790, 462210, 554, 0, FALSE, 0),
    (3018, 'HD3018', 1004, 1001, 3001, '2024-12-14 19:48:00', 140000, 9800, 7000, 16800, 123200, 147, 7, TRUE, 7),
    (3019, 'HD3019', 1006, 1001, 3001, '2024-12-18 14:01:00', 123000, 8610, 0, 8610, 114390, 137, 0, FALSE, 0),
    (3020, 'HD3020', 1007, 1001, 3001, '2024-12-25 09:14:00', 292000, 20440, 0, 20440, 271560, 325, 0, FALSE, 0),
    (3021, 'HD3021', 1009, 1001, 3002, '2025-01-06 16:27:00', 269000, 26900, 0, 26900, 242100, 363, 0, FALSE, 0),
    (3022, 'HD3022', 1001, 1001, 3002, '2025-01-14 11:40:00', 417000, 41700, 11000, 52700, 364300, 546, 11, TRUE, 11),
    (3023, 'HD3023', 1002, 1, 3002, '2025-01-18 18:53:00', 530000, 53000, 0, 53000, 477000, 715, 0, FALSE, 0),
    (3024, 'HD3024', 1003, 1001, 3002, '2025-01-25 13:06:00', 117000, 11700, 0, 11700, 105300, 157, 0, FALSE, 0),
    (3025, 'HD3025', 1004, 1001, 3002, '2025-02-06 08:19:00', 499000, 49900, 0, 49900, 449100, 673, 0, FALSE, 0),
    (3026, 'HD3026', 1006, 1001, 3002, '2025-02-14 15:32:00', 210000, 21000, 15000, 36000, 174000, 261, 15, TRUE, 15),
    (3027, 'HD3027', 1007, 1, 3002, '2025-02-18 10:45:00', 83000, 8300, 0, 8300, 74700, 112, 0, FALSE, 0),
    (3028, 'HD3028', 1009, 1002, NULL, '2025-02-25 17:58:00', 80000, 0, 0, 0, 80000, 80, 0, FALSE, 0),
    (3029, 'HD3029', 1001, 1, NULL, '2025-03-06 12:11:00', 212000, 0, 0, 0, 212000, 212, 0, FALSE, 0),
    (3030, 'HD3030', 1002, 1002, NULL, '2025-03-14 19:24:00', 190000, 0, 19000, 19000, 171000, 171, 19, TRUE, 19),
    (3031, 'HD3031', 1003, 1001, NULL, '2025-03-18 14:37:00', 150000, 0, 0, 0, 150000, 150, 0, FALSE, 0),
    (3032, 'HD3032', 1004, 1, NULL, '2025-03-25 09:50:00', 473000, 0, 0, 0, 473000, 473, 0, FALSE, 0),
    (3033, 'HD3033', 1006, 1001, NULL, '2025-04-06 16:03:00', 140000, 0, 0, 0, 140000, 140, 0, FALSE, 0),
    (3034, 'HD3034', 1007, 1, NULL, '2025-04-14 11:16:00', 130000, 0, 23000, 23000, 107000, 107, 23, TRUE, 23),
    (3035, 'HD3035', 1009, 1002, NULL, '2025-04-18 18:29:00', 916000, 0, 0, 0, 916000, 916, 0, FALSE, 0),
    (3036, 'HD3036', 1001, 1001, NULL, '2025-04-25 13:42:00', 561000, 0, 0, 0, 561000, 561, 0, FALSE, 0),
    (3037, 'HD3037', 1002, 1002, NULL, '2025-05-06 08:55:00', 278000, 0, 0, 0, 278000, 278, 0, FALSE, 0),
    (3038, 'HD3038', 1003, 1002, NULL, '2025-05-14 15:08:00', 561000, 0, 10000, 10000, 551000, 551, 10, TRUE, 10),
    (3039, 'HD3039', 1004, 1003, NULL, '2025-05-18 10:21:00', 167000, 0, 0, 0, 167000, 167, 0, FALSE, 0),
    (3040, 'HD3040', 1006, 1001, NULL, '2025-05-25 17:34:00', 43000, 0, 0, 0, 43000, 43, 0, FALSE, 0),
    (3041, 'HD3041', 1007, 1002, 3003, '2025-06-06 12:47:00', 136000, 10880, 0, 10880, 125120, 162, 0, FALSE, 0),
    (3042, 'HD3042', 1009, 1003, 3003, '2025-06-14 19:00:00', 252000, 20160, 14000, 34160, 217840, 283, 14, TRUE, 14),
    (3043, 'HD3043', 1001, 1001, 3003, '2025-06-18 14:13:00', 40000, 3200, 0, 3200, 36800, 47, 0, FALSE, 0),
    (3044, 'HD3044', 1002, 1, 3003, '2025-06-25 09:26:00', 280000, 22400, 0, 22400, 257600, 334, 0, FALSE, 0),
    (3045, 'HD3045', 1003, 1003, 3003, '2025-07-06 16:39:00', 235000, 18800, 0, 18800, 216200, 281, 0, FALSE, 0),
    (3046, 'HD3046', 1004, 1001, 3003, '2025-07-14 11:52:00', 75000, 6000, 18000, 24000, 51000, 66, 18, TRUE, 18),
    (3047, 'HD3047', 1006, 1, 3003, '2025-07-18 18:05:00', 269000, 21520, 0, 21520, 247480, 321, 0, FALSE, 0),
    (3048, 'HD3048', 1007, 1002, 3003, '2025-07-25 13:18:00', 916000, 73280, 0, 73280, 842720, 1095, 0, FALSE, 0),
    (3049, 'HD3049', 1009, 1001, 3003, '2025-08-06 08:31:00', 65000, 5200, 0, 5200, 59800, 77, 0, FALSE, 0),
    (3050, 'HD3050', 1001, 1, 3003, '2025-08-14 15:44:00', 777000, 62160, 22000, 84160, 692840, 900, 22, TRUE, 22),
    (3051, 'HD3051', 1002, 1002, 3003, '2025-08-18 10:57:00', 530000, 42400, 0, 42400, 487600, 633, 0, FALSE, 0),
    (3052, 'HD3052', 1003, 1003, 3003, '2025-08-25 17:10:00', 124000, 9920, 0, 9920, 114080, 148, 0, FALSE, 0),
    (3053, 'HD3053', 1004, 1, NULL, '2025-09-06 12:23:00', 530000, 0, 0, 0, 530000, 530, 0, FALSE, 0),
    (3054, 'HD3054', 1006, 1002, NULL, '2025-09-14 19:36:00', 179000, 0, 9000, 9000, 170000, 170, 9, TRUE, 9),
    (3055, 'HD3055', 1007, 1003, NULL, '2025-09-18 14:49:00', 172000, 0, 0, 0, 172000, 172, 0, FALSE, 0),
    (3056, 'HD3056', 1009, 1001, NULL, '2025-09-25 09:02:00', 190000, 0, 0, 0, 190000, 190, 0, FALSE, 0),
    (3057, 'HD3057', 1001, 1002, NULL, '2025-10-06 16:15:00', 169000, 0, 0, 0, 169000, 169, 0, FALSE, 0),
    (3058, 'HD3058', 1002, 1003, NULL, '2025-10-14 11:28:00', 160000, 0, 13000, 13000, 147000, 147, 13, TRUE, 13),
    (3059, 'HD3059', 1003, 1001, NULL, '2025-10-18 18:41:00', 140000, 0, 0, 0, 140000, 140, 0, FALSE, 0),
    (3060, 'HD3060', 1004, 1, NULL, '2025-10-25 13:54:00', 408000, 0, 0, 0, 408000, 408, 0, FALSE, 0),
    (3061, 'HD3061', 1006, 1003, NULL, '2025-11-06 08:07:00', 300000, 0, 0, 0, 300000, 300, 0, FALSE, 0),
    (3062, 'HD3062', 1007, 1001, NULL, '2025-11-14 15:20:00', 343000, 0, 17000, 17000, 326000, 326, 17, TRUE, 17),
    (3063, 'HD3063', 1009, 1, NULL, '2025-11-18 10:33:00', 777000, 0, 0, 0, 777000, 777, 0, FALSE, 0),
    (3064, 'HD3064', 1001, 1002, NULL, '2025-11-25 17:46:00', 499000, 0, 0, 0, 499000, 499, 0, FALSE, 0),
    (3065, 'HD3065', 1002, 1001, NULL, '2025-12-06 12:59:00', 638000, 0, 0, 0, 638000, 638, 0, FALSE, 0),
    (3066, 'HD3066', 1003, 1, NULL, '2025-12-14 19:12:00', 561000, 0, 21000, 21000, 540000, 540, 21, TRUE, 21),
    (3067, 'HD3067', 1004, 1002, NULL, '2025-12-18 14:25:00', 93000, 0, 0, 0, 93000, 93, 0, FALSE, 0),
    (3068, 'HD3068', 1006, 1003, NULL, '2025-12-25 09:38:00', 252000, 0, 0, 0, 252000, 252, 0, FALSE, 0),
    (3069, 'HD3069', 1007, 1, 1006, '2026-01-06 16:51:00', 224000, 0, 0, 0, 224000, 224, 0, FALSE, 0),
    (3070, 'HD3070', 1009, 1002, 1006, '2026-01-16 11:04:00', 204000, 0, 8000, 8000, 196000, 196, 8, TRUE, 8),
    (3071, 'HD3071', 1001, 1003, 1006, '2026-01-25 18:17:00', 1100000, 0, 0, 0, 1100000, 1100, 0, FALSE, 0),
    (3072, 'HD3072', 1002, 1, 1006, '2026-02-06 13:30:00', 426000, 0, 0, 0, 426000, 426, 0, FALSE, 0),
    (3073, 'HD3073', 1003, 1002, 1006, '2026-02-16 08:43:00', 435000, 0, 0, 0, 435000, 435, 0, FALSE, 0),
    (3074, 'HD3074', 1004, 1003, 1006, '2026-02-25 15:56:00', 584000, 0, 12000, 12000, 572000, 572, 12, TRUE, 12),
    (3075, 'HD3075', 1006, 1, 1006, '2026-03-06 10:09:00', 810000, 0, 0, 0, 810000, 810, 0, FALSE, 0),
    (3076, 'HD3076', 1007, 1002, 1006, '2026-03-16 17:22:00', 520000, 0, 0, 0, 520000, 520, 0, FALSE, 0),
    (3077, 'HD3077', 1009, 1003, 1006, '2026-03-25 12:35:00', 173000, 0, 0, 0, 173000, 173, 0, FALSE, 0),
    (3078, 'HD3078', 1001, 1, 1006, '2026-04-06 19:48:00', 584000, 0, 16000, 16000, 568000, 568, 16, TRUE, 16),
    (3079, 'HD3079', 1002, 1002, 1006, '2026-04-16 14:01:00', 96000, 0, 0, 0, 96000, 96, 0, FALSE, 0),
    (3080, 'HD3080', 1003, 1003, 1006, '2026-04-25 09:14:00', 264000, 0, 0, 0, 264000, 264, 0, FALSE, 0),
    (3081, 'HD3081', 1004, 1, 1006, '2026-05-06 16:27:00', 109000, 0, 0, 0, 109000, 109, 0, FALSE, 0),
    (3082, 'HD3082', 1006, 1002, 1006, '2026-05-16 11:40:00', 135000, 0, 20000, 20000, 115000, 115, 20, TRUE, 20),
    (3083, 'HD3083', 1007, 1003, 1006, '2026-05-25 18:53:00', 246000, 0, 0, 0, 246000, 246, 0, FALSE, 0),
    (3084, 'HD3084', 1009, 1, 1006, '2026-06-06 13:06:00', 174000, 0, 0, 0, 174000, 174, 0, FALSE, 0),
    (3085, 'HD3085', 1001, 1002, 1006, '2026-06-16 08:19:00', 126000, 0, 0, 0, 126000, 126, 0, FALSE, 0),
    (3086, 'HD3086', 1002, 1003, 1006, '2026-06-25 15:32:00', 448000, 0, 7000, 7000, 441000, 441, 7, TRUE, 7),
    (3087, 'HD3087', 1003, 1, 1006, '2026-07-06 10:45:00', 162000, 0, 0, 0, 162000, 162, 0, FALSE, 0),
    (3088, 'HD3088', 1004, 1002, 1006, '2026-07-16 17:58:00', 234000, 0, 0, 0, 234000, 234, 0, FALSE, 0),
    (3089, 'HD3089', 1006, 1003, 1006, '2026-07-25 12:11:00', 417000, 0, 0, 0, 417000, 417, 0, FALSE, 0),
    (3090, 'HD3090', 1007, 1, 1006, '2026-08-06 19:24:00', 292000, 0, 11000, 11000, 281000, 281, 11, TRUE, 11),
    (3091, 'HD3091', 1009, 1002, 1006, '2026-08-16 14:37:00', 204000, 0, 0, 0, 204000, 204, 0, FALSE, 0),
    (3092, 'HD3092', 1001, 1003, 1006, '2026-08-25 09:50:00', 1100000, 0, 0, 0, 1100000, 1100, 0, FALSE, 0),
    (3093, 'HD3093', 1002, 1, 1006, '2026-09-06 16:03:00', 281000, 0, 0, 0, 281000, 281, 0, FALSE, 0),
    (3094, 'HD3094', 1003, 1002, 1002, '2026-09-16 11:16:00', 435000, 87000, 15000, 102000, 333000, 666, 15, TRUE, 15),
    (3095, 'HD3095', 1004, 1003, 1002, '2026-09-25 18:29:00', 552000, 110400, 0, 110400, 441600, 883, 0, FALSE, 0),
    (3096, 'HD3096', 1006, 1, 1002, '2026-10-06 13:42:00', 810000, 162000, 0, 162000, 648000, 1296, 0, FALSE, 0),
    (3097, 'HD3097', 1007, 1002, 1002, '2026-10-16 08:55:00', 520000, 104000, 0, 104000, 416000, 832, 0, FALSE, 0),
    (3098, 'HD3098', 1009, 1003, 1002, '2026-10-25 15:08:00', 218000, 43600, 15200, 58800, 159200, 318, 19, TRUE, 19),
    (3099, 'HD3099', 1001, 1, 1002, '2026-11-06 10:21:00', 552000, 110400, 0, 110400, 441600, 883, 0, FALSE, 0),
    (3100, 'HD3100', 1002, 1002, 1002, '2026-11-16 17:34:00', 96000, 19200, 0, 19200, 76800, 153, 0, FALSE, 0);

INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, line_total)
VALUES
    (3001, 1003, 1, 131000, 131000),
    (3002, 1006, 1, 468000, 468000),
    (3002, 1002, 2, 29000, 58000),
    (3003, 1002, 3, 29000, 87000),
    (3003, 1005, 1, 41000, 41000),
    (3004, 1005, 4, 41000, 164000),
    (3005, 1002, 2, 29000, 58000),
    (3005, 1005, 1, 41000, 41000),
    (3006, 1005, 3, 41000, 123000),
    (3006, 1008, 2, 38000, 76000),
    (3007, 1008, 4, 38000, 152000),
    (3008, 1004, 1, 70000, 70000),
    (3008, 1007, 2, 61000, 122000),
    (3009, 1008, 3, 38000, 114000),
    (3009, 1004, 1, 70000, 70000),
    (3010, 1004, 4, 70000, 280000),
    (3011, 1007, 1, 61000, 61000),
    (3011, 1003, 1, 131000, 131000),
    (3012, 1003, 2, 131000, 262000),
    (3012, 1006, 1, 468000, 468000),
    (3013, 1007, 4, 61000, 244000),
    (3014, 1003, 1, 131000, 131000),
    (3014, 1006, 1, 468000, 468000),
    (3015, 1006, 1, 468000, 468000),
    (3015, 1002, 1, 29000, 29000),
    (3016, 1002, 3, 29000, 87000),
    (3017, 1006, 1, 468000, 468000),
    (3017, 1002, 1, 29000, 29000),
    (3018, 1002, 2, 29000, 58000),
    (3018, 1005, 2, 41000, 82000),
    (3019, 1005, 3, 41000, 123000),
    (3020, 1008, 4, 38000, 152000),
    (3020, 1004, 2, 70000, 140000),
    (3021, 1007, 2, 65000, 130000),
    (3021, 1003, 1, 139000, 139000),
    (3022, 1003, 3, 139000, 417000),
    (3023, 1006, 1, 499000, 499000),
    (3023, 1002, 1, 31000, 31000),
    (3024, 1002, 1, 31000, 31000),
    (3024, 1005, 2, 43000, 86000),
    (3025, 1006, 1, 499000, 499000),
    (3026, 1002, 4, 31000, 124000),
    (3026, 1005, 2, 43000, 86000),
    (3027, 1005, 1, 43000, 43000),
    (3027, 1008, 1, 40000, 40000),
    (3028, 1008, 2, 40000, 80000),
    (3029, 1005, 4, 43000, 172000),
    (3029, 1008, 1, 40000, 40000),
    (3030, 1008, 1, 40000, 40000),
    (3030, 1004, 2, 75000, 150000),
    (3031, 1004, 2, 75000, 150000),
    (3032, 1007, 3, 65000, 195000),
    (3032, 1003, 2, 139000, 278000),
    (3033, 1004, 1, 75000, 75000),
    (3033, 1007, 1, 65000, 65000),
    (3034, 1007, 2, 65000, 130000),
    (3035, 1003, 3, 139000, 417000),
    (3035, 1006, 1, 499000, 499000),
    (3036, 1006, 1, 499000, 499000),
    (3036, 1002, 2, 31000, 62000),
    (3037, 1003, 2, 139000, 278000),
    (3038, 1006, 1, 499000, 499000),
    (3038, 1002, 2, 31000, 62000),
    (3039, 1002, 4, 31000, 124000),
    (3039, 1005, 1, 43000, 43000),
    (3040, 1005, 1, 43000, 43000),
    (3041, 1002, 3, 31000, 93000),
    (3041, 1005, 1, 43000, 43000),
    (3042, 1005, 4, 43000, 172000),
    (3042, 1008, 2, 40000, 80000),
    (3043, 1008, 1, 40000, 40000),
    (3044, 1004, 2, 75000, 150000),
    (3044, 1007, 2, 65000, 130000),
    (3045, 1008, 4, 40000, 160000),
    (3045, 1004, 1, 75000, 75000),
    (3046, 1004, 1, 75000, 75000),
    (3047, 1007, 2, 65000, 130000),
    (3047, 1003, 1, 139000, 139000),
    (3048, 1003, 3, 139000, 417000),
    (3048, 1006, 1, 499000, 499000),
    (3049, 1007, 1, 65000, 65000),
    (3050, 1003, 2, 139000, 278000),
    (3050, 1006, 1, 499000, 499000),
    (3051, 1006, 1, 499000, 499000),
    (3051, 1002, 1, 31000, 31000),
    (3052, 1002, 4, 31000, 124000),
    (3053, 1006, 1, 499000, 499000),
    (3053, 1002, 1, 31000, 31000),
    (3054, 1002, 3, 31000, 93000),
    (3054, 1005, 2, 43000, 86000),
    (3055, 1005, 4, 43000, 172000),
    (3056, 1008, 1, 40000, 40000),
    (3056, 1004, 2, 75000, 150000),
    (3057, 1005, 3, 43000, 129000),
    (3057, 1008, 1, 40000, 40000),
    (3058, 1008, 4, 40000, 160000),
    (3059, 1004, 1, 75000, 75000),
    (3059, 1007, 1, 65000, 65000),
    (3060, 1007, 2, 65000, 130000),
    (3060, 1003, 2, 139000, 278000),
    (3061, 1004, 4, 75000, 300000),
    (3062, 1007, 1, 65000, 65000),
    (3062, 1003, 2, 139000, 278000),
    (3063, 1003, 2, 139000, 278000),
    (3063, 1006, 1, 499000, 499000),
    (3064, 1006, 1, 499000, 499000),
    (3065, 1003, 1, 139000, 139000),
    (3065, 1006, 1, 499000, 499000),
    (3066, 1006, 1, 499000, 499000),
    (3066, 1002, 2, 31000, 62000),
    (3067, 1002, 3, 31000, 93000),
    (3068, 1005, 4, 43000, 172000),
    (3068, 1008, 2, 40000, 80000),
    (3069, 1004, 2, 78000, 156000),
    (3069, 1007, 1, 68000, 68000),
    (3070, 1007, 3, 68000, 204000),
    (3071, 1003, 4, 145000, 580000),
    (3071, 1006, 1, 520000, 520000),
    (3072, 1007, 2, 68000, 136000),
    (3072, 1003, 2, 145000, 290000),
    (3073, 1003, 3, 145000, 435000),
    (3074, 1006, 1, 520000, 520000),
    (3074, 1002, 2, 32000, 64000),
    (3075, 1003, 2, 145000, 290000),
    (3075, 1006, 1, 520000, 520000),
    (3076, 1006, 1, 520000, 520000),
    (3077, 1002, 4, 32000, 128000),
    (3077, 1005, 1, 45000, 45000),
    (3078, 1006, 1, 520000, 520000),
    (3078, 1002, 2, 32000, 64000),
    (3079, 1002, 3, 32000, 96000),
    (3080, 1005, 4, 45000, 180000),
    (3080, 1008, 2, 42000, 84000),
    (3081, 1002, 2, 32000, 64000),
    (3081, 1005, 1, 45000, 45000),
    (3082, 1005, 3, 45000, 135000),
    (3083, 1008, 4, 42000, 168000),
    (3083, 1004, 1, 78000, 78000),
    (3084, 1005, 2, 45000, 90000),
    (3084, 1008, 2, 42000, 84000),
    (3085, 1008, 3, 42000, 126000),
    (3086, 1004, 4, 78000, 312000),
    (3086, 1007, 2, 68000, 136000),
    (3087, 1008, 2, 42000, 84000),
    (3087, 1004, 1, 78000, 78000),
    (3088, 1004, 3, 78000, 234000),
    (3089, 1007, 4, 68000, 272000),
    (3089, 1003, 1, 145000, 145000),
    (3090, 1004, 2, 78000, 156000),
    (3090, 1007, 2, 68000, 136000),
    (3091, 1007, 3, 68000, 204000),
    (3092, 1003, 4, 145000, 580000),
    (3092, 1006, 1, 520000, 520000),
    (3093, 1007, 2, 68000, 136000),
    (3093, 1003, 1, 145000, 145000),
    (3094, 1003, 3, 145000, 435000),
    (3095, 1006, 1, 520000, 520000),
    (3095, 1002, 1, 32000, 32000),
    (3096, 1003, 2, 145000, 290000),
    (3096, 1006, 1, 520000, 520000),
    (3097, 1006, 1, 520000, 520000),
    (3098, 1002, 4, 32000, 128000),
    (3098, 1005, 2, 45000, 90000),
    (3099, 1006, 1, 520000, 520000),
    (3099, 1002, 1, 32000, 32000),
    (3100, 1002, 3, 32000, 96000);

-- Moi nam co lo rieng cho cac hoa don lich su. Phieu nhap theo nha cung cap cung nam.
INSERT INTO inventory_batches (batch_id, product_id, location_id, batch_code, expiry_date, quantity_on_hand)
SELECT DISTINCT 3000 + (YEAR(sale.sold_at) - 2024) * 10 + item.product_id - 1000,
       item.product_id, existing_batch.location_id,
       CONCAT('LOT-MAU-', YEAR(sale.sold_at), '-', item.product_id),
       MAKEDATE(YEAR(sale.sold_at) + 1, 1) - INTERVAL 1 DAY, 0
FROM sales sale
JOIN sale_items item ON item.sale_id = sale.sale_id
JOIN inventory_batches existing_batch ON existing_batch.batch_id = item.product_id
WHERE sale.sale_id BETWEEN 3001 AND 3100;

INSERT INTO goods_receipts (receipt_id, receipt_code, supplier_id, received_by, received_at, status, note)
SELECT DISTINCT 4000 + (YEAR(batch.expiry_date) - 2024) * 10 + supply.supplier_id - 1000,
       CONCAT('PN-MAU-', YEAR(batch.expiry_date), '-', supply.supplier_id), supply.supplier_id, 1001,
       TIMESTAMP(CASE WHEN YEAR(batch.expiry_date) = 2024 THEN DATE('2024-08-05')
                      ELSE MAKEDATE(YEAR(batch.expiry_date), 3) END, '08:30:00'),
       'RECEIVED', 'Nhập lô cho hóa đơn lịch sử mẫu'
FROM inventory_batches batch
JOIN supplier_products supply ON supply.product_id = batch.product_id
WHERE batch.batch_id BETWEEN 3002 AND 3028;

INSERT INTO goods_receipt_items (receipt_id, batch_id, quantity, unit_cost)
SELECT receipt.receipt_id, batch.batch_id, SUM(item.quantity), supply.purchase_price
FROM sales sale
JOIN sale_items item ON item.sale_id = sale.sale_id
JOIN inventory_batches batch
  ON batch.batch_id = 3000 + (YEAR(sale.sold_at) - 2024) * 10 + item.product_id - 1000
JOIN supplier_products supply ON supply.product_id = batch.product_id
JOIN goods_receipts receipt
  ON receipt.receipt_id = 4000 + (YEAR(sale.sold_at) - 2024) * 10 + supply.supplier_id - 1000
WHERE sale.sale_id BETWEEN 3001 AND 3100
GROUP BY receipt.receipt_id, batch.batch_id, supply.purchase_price;

INSERT INTO inventory_movements
    (batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
SELECT item.batch_id, 'RECEIPT', item.quantity, 'GOODS_RECEIPT', receipt.receipt_id,
       receipt.received_at, receipt.received_by, CONCAT('Nhập lô theo ', receipt.receipt_code)
FROM goods_receipt_items item
JOIN goods_receipts receipt ON receipt.receipt_id = item.receipt_id
WHERE receipt.receipt_id BETWEEN 4001 AND 4025;

INSERT INTO inventory_movements
    (batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
SELECT batch.batch_id, 'SALE', -item.quantity, 'SALE', sale.sale_id,
       sale.sold_at, employee.user_id, CONCAT('Xuất lô theo ', sale.sale_code)
FROM sales sale
JOIN sale_items item ON item.sale_id = sale.sale_id
JOIN inventory_batches batch
  ON batch.batch_id = 3000 + (YEAR(sale.sold_at) - 2024) * 10 + item.product_id - 1000
JOIN employees employee ON employee.employee_id = sale.employee_id
WHERE sale.sale_id BETWEEN 3001 AND 3100;

-- Kiem ke da chot co lech ton, phieu cho kiem va phieu da huy.
INSERT INTO stocktakes (stocktake_id, location_id, stocktake_date, status, created_by, note)
VALUES
    (1001, 1, '2026-11-24', 'COMPLETED', 1, 'Kiểm kê cuối ngày ở quầy A'),
    (1002, 2, CURRENT_DATE(), 'DRAFT', 1003, 'Chờ đối chiếu số đếm tại kệ B'),
    (1003, 1003, '2026-11-21', 'CANCELLED', 1, 'Dời lịch kiểm kê thiết bị');

INSERT INTO stocktake_items (stocktake_id, batch_id, expected_quantity, counted_quantity, difference_quantity)
SELECT 1001, batch_id, SUM(quantity_delta),
       SUM(quantity_delta) - CASE WHEN batch_id = 1002 THEN 2 ELSE 0 END,
       CASE WHEN batch_id = 1002 THEN -2 ELSE 0 END
FROM inventory_movements
WHERE batch_id IN (1002, 1005)
  AND moved_at < '2026-11-25 00:00:00'
GROUP BY batch_id;

INSERT INTO stocktake_items (stocktake_id, batch_id, expected_quantity, counted_quantity, difference_quantity)
SELECT 1002, batch_id, SUM(quantity_delta), SUM(quantity_delta), 0
FROM inventory_movements
WHERE batch_id = 1003
GROUP BY batch_id;

INSERT INTO inventory_movements
    (batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
SELECT batch_id, 'ADJUSTMENT', difference_quantity, 'STOCKTAKE', stocktake_id,
       '2026-11-24 21:10:00', 1, 'Điều chỉnh theo phiếu kiểm kê KK1001'
FROM stocktake_items
WHERE stocktake_id = 1001 AND difference_quantity <> 0;

-- Doi soat so du diem va ton kho tu nhung giao dich da ghi nhan.
UPDATE customers customer
LEFT JOIN (
    SELECT customer_id, MIN(sold_at) AS first_sale,
           SUM(CAST(points_earned AS SIGNED) - CAST(points_redeemed AS SIGNED)) AS balance
    FROM sales
    WHERE status = 'COMPLETED'
    GROUP BY customer_id
) loyalty ON loyalty.customer_id = customer.customer_id
SET customer.loyalty_points = COALESCE(loyalty.balance, 0),
    customer.created_at = COALESCE(DATE_SUB(loyalty.first_sale, INTERVAL 2 DAY), customer.created_at)
WHERE customer.customer_id >= 1001;

UPDATE inventory_batches batch
JOIN (
    SELECT batch_id, SUM(quantity_delta) AS quantity
    FROM inventory_movements
    GROUP BY batch_id
) movement ON movement.batch_id = batch.batch_id
SET batch.quantity_on_hand = movement.quantity;

COMMIT;
