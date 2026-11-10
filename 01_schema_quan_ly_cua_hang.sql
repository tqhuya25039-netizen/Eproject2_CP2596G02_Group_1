-- CSDL quan ly cua hang: nhan su, san pham, kho va ban hang.
-- Yeu cau: MySQL 8.0.16+ (de ap dung day du cac CHECK constraint).
-- Chay tren mot database moi; khong chay lai tren database da co cac bang nay.

CREATE DATABASE IF NOT EXISTS quan_ly_cua_hang
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE quan_ly_cua_hang;

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
    unit_name VARCHAR(30) NOT NULL,
    requires_prescription TINYINT(1) NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_id),
    UNIQUE KEY uq_products_code (product_code),
    UNIQUE KEY uq_products_barcode (barcode),
    KEY ix_products_category (category_id),
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES product_categories (category_id),
    CONSTRAINT chk_products_prescription CHECK (requires_prescription IN (0, 1)),
    CONSTRAINT chk_products_is_active CHECK (is_active IN (0, 1))
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
    loyalty_points INT UNSIGNED NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customers_code (customer_code),
    UNIQUE KEY uq_customers_phone (phone),
    CONSTRAINT chk_customers_is_active CHECK (is_active IN (0, 1))
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
    ends_at DATETIME NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (promotion_id),
    UNIQUE KEY uq_promotions_code (promotion_code),
    CONSTRAINT chk_promotions_discount CHECK (discount_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_promotions_multiplier CHECK (points_multiplier >= 0),
    CONSTRAINT chk_promotions_dates CHECK (starts_at < ends_at),
    CONSTRAINT chk_promotions_is_active CHECK (is_active IN (0, 1))
) ENGINE=InnoDB;

CREATE TABLE sales (
    sale_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sale_code VARCHAR(30) NOT NULL,
    customer_id BIGINT UNSIGNED NULL,
    employee_id BIGINT UNSIGNED NOT NULL,
    promotion_id BIGINT UNSIGNED NULL,
    sold_at DATETIME NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_amount DECIMAL(12, 2) NOT NULL,
    points_earned INT UNSIGNED NOT NULL DEFAULT 0,
    points_redeemed INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
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
    CONSTRAINT chk_sales_status CHECK (status IN ('COMPLETED', 'VOIDED'))
) ENGINE=InnoDB;

CREATE TABLE sale_items (
    sale_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sale_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    line_discount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    line_total DECIMAL(12, 2) NOT NULL,
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
        CHECK (line_total = quantity * unit_price - line_discount)
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

-- Du lieu mau: moi bang co tu 1 den 10 dong, cac khoa ngoai deu tro den du lieu ton tai.
INSERT INTO roles (role_id, role_code, role_name, description) VALUES
    (1, 'ADMIN', 'Quan tri vien', 'Quan ly tai khoan va toan bo nghiep vu'),
    (2, 'STAFF', 'Nhan vien', 'Thuc hien nghiep vu duoc phan cong');

-- password_hash la gia tri mau, khong phai hash dang nhap hop le.
-- Hay thay bang bcrypt/Argon2 hash duoc tao boi ung dung truoc khi su dung that.
INSERT INTO users (user_id, role_id, username, password_hash, is_active) VALUES
    (1, 1, 'admin_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1),
    (2, 2, 'staff_demo', 'REPLACE_WITH_REAL_PASSWORD_HASH', 1);

INSERT INTO employees
    (employee_id, user_id, employee_code, full_name, phone, email, hire_date, employment_status)
VALUES
    (1, 2, 'NV0001', 'Nguyen Thi An', '0900000001', 'an.demo@example.com', '2025-01-15', 'ACTIVE');

INSERT INTO shifts (shift_id, shift_name, start_time, end_time, break_minutes, is_active) VALUES
    (1, 'Ca sang', '08:00:00', '12:00:00', 0, 1),
    (2, 'Ca chieu', '13:00:00', '17:00:00', 0, 1);

INSERT INTO employee_availabilities
    (availability_id, employee_id, available_date, start_time, end_time, note)
VALUES
    (1, 1, '2026-11-02', '08:00:00', '16:00:00', 'Dang ky ca cho tuan tiep theo');

INSERT INTO work_schedules (schedule_id, week_start_date, status, created_by) VALUES
    (1, '2026-11-02', 'PUBLISHED', 1);

INSERT INTO schedule_assignments
    (assignment_id, schedule_id, employee_id, shift_id, work_date, status, note)
VALUES
    (1, 1, 1, 1, '2026-11-02', 'COMPLETED', NULL);

INSERT INTO attendance_records (attendance_id, assignment_id, check_in_at, check_out_at, note) VALUES
    (1, 1, '2026-11-02 08:02:00', '2026-11-02 12:05:00', 'Nhan vien den muon 2 phut');

INSERT INTO product_categories (category_id, category_name, description, is_active) VALUES
    (1, 'Thuoc khong ke don', 'Nhom san pham khong yeu cau don thuoc', 1),
    (2, 'Thuc pham bao ve suc khoe', 'Vitamin va san pham bo sung', 1);

INSERT INTO suppliers (supplier_id, supplier_code, supplier_name, phone, email, address, is_active) VALUES
    (1, 'NCC001', 'Cong ty Duoc Minh Tam', '02830000001', 'sales@minhtam.example', 'TP Ho Chi Minh', 1),
    (2, 'NCC002', 'Nha phan phoi An Khang', '02830000002', 'contact@ankhang.example', 'TP Ho Chi Minh', 1);

INSERT INTO products
    (product_id, category_id, product_code, barcode, product_name, dosage_form, unit_name,
     requires_prescription, is_active)
VALUES
    (1, 1, 'SP0001', '8930000000001', 'Paracetamol 500 mg', 'Vien nen', 'vi', 0, 1),
    (2, 2, 'SP0002', '8930000000002', 'Vitamin C 500 mg', 'Vien nen', 'hop', 0, 1);

INSERT INTO supplier_products (supplier_id, product_id, supplier_sku, purchase_price, lead_time_days) VALUES
    (1, 1, 'MT-PARA-500', 1000.00, 2),
    (2, 2, 'AK-VITC-500', 35000.00, 3);

INSERT INTO warehouse_locations (location_id, location_code, location_name, description, is_active) VALUES
    (1, 'KE-A1', 'Ke A tang 1', 'Khu trung bay thuoc khong ke don', 1),
    (2, 'KE-B1', 'Ke B tang 1', 'Khu trung bay thuc pham bao ve suc khoe', 1);

INSERT INTO inventory_batches
    (batch_id, product_id, location_id, batch_code, expiry_date, quantity_on_hand)
VALUES
    (1, 1, 1, 'LOT-PARA-001', '2027-12-31', 97.000),
    (2, 2, 2, 'LOT-VITC-001', '2028-01-31', 50.000);

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
    (customer_id, customer_code, full_name, phone, email, loyalty_points, is_active)
VALUES
    (1, 'KH0001', 'Tran Minh Binh', '0910000001', 'binh.demo@example.com', 4, 1);

INSERT INTO customer_allergies (customer_allergy_id, customer_id, allergen_name, note) VALUES
    (1, 1, 'Penicillin', 'Khach hang khai bao di ung');

INSERT INTO loyalty_programs
    (loyalty_program_id, program_name, points_per_1000_currency, currency_per_point, is_active)
VALUES
    (1, 'Tich diem co ban', 1.0000, 1000.00, 1);

INSERT INTO promotions
    (promotion_id, promotion_code, promotion_name, discount_percent, points_multiplier,
     starts_at, ends_at, is_active)
VALUES
    (1, 'WELCOME10', 'Uu dai khach hang moi', 10.00, 1.50,
     '2026-10-01 00:00:00', '2026-12-31 23:59:59', 1);

INSERT INTO sales
    (sale_id, sale_code, customer_id, employee_id, promotion_id, sold_at, subtotal,
     discount_amount, total_amount, points_earned, points_redeemed, status)
VALUES
    (1, 'HD0001', 1, 1, 1, '2026-10-27 10:00:00', 5000.00, 500.00, 4500.00, 4, 0, 'COMPLETED');

INSERT INTO sale_items
    (sale_item_id, sale_id, product_id, quantity, unit_price, line_discount, line_total)
VALUES
    (1, 1, 1, 2.000, 2500.00, 500.00, 4500.00);

INSERT INTO inventory_movements
    (movement_id, batch_id, movement_type, quantity_delta, source_type, source_id, moved_at, created_by, note)
VALUES
    (1, 1, 'RECEIPT', 100.000, 'GOODS_RECEIPT', 1, '2026-10-27 09:00:00', 2, 'Nhap kho theo PN0001'),
    (2, 1, 'SALE', -2.000, 'SALE', 1, '2026-10-27 10:00:00', 2, 'Ban hang theo HD0001'),
    (3, 1, 'ADJUSTMENT', -1.000, 'STOCKTAKE', 1, '2026-10-27 17:00:00', 1, 'Dieu chinh sau kiem ke');
