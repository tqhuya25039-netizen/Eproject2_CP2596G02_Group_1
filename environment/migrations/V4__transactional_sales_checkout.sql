-- Back up the existing database before running this migration once.
-- Do not run this migration automatically from the application.
ALTER TABLE products
    ADD COLUMN selling_price DECIMAL(12, 2) NULL AFTER requires_prescription,
    ADD CONSTRAINT chk_products_selling_price
        CHECK (selling_price IS NULL OR selling_price >= 0);

CREATE TABLE product_ingredients (
    product_id BIGINT UNSIGNED NOT NULL,
    ingredient_name VARCHAR(120) NOT NULL,
    PRIMARY KEY (product_id, ingredient_name),
    CONSTRAINT fk_product_ingredients_product
        FOREIGN KEY (product_id) REFERENCES products (product_id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE customers
    ADD COLUMN date_of_birth DATE NULL AFTER email,
    ADD KEY ix_customers_email (email);

ALTER TABLE sales
    ADD COLUMN promotion_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 AFTER subtotal,
    ADD COLUMN points_discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 AFTER promotion_discount_amount,
    ADD COLUMN prescription_code VARCHAR(80) NULL AFTER status,
    ADD COLUMN prescription_date DATE NULL AFTER prescription_code,
    ADD COLUMN prescribing_doctor VARCHAR(120) NULL AFTER prescription_date,
    ADD COLUMN clinic VARCHAR(180) NULL AFTER prescribing_doctor,
    ADD COLUMN diagnosis VARCHAR(255) NULL AFTER clinic,
    ADD COLUMN note VARCHAR(500) NULL AFTER diagnosis;

ALTER TABLE sale_items
    ADD COLUMN dosage_instructions VARCHAR(255) NULL AFTER line_total,
    ADD COLUMN treatment_days SMALLINT UNSIGNED NOT NULL DEFAULT 1 AFTER dosage_instructions,
    DROP CHECK chk_sale_items_total,
    ADD CONSTRAINT chk_sale_items_total
        CHECK (line_total = ROUND(quantity * unit_price - line_discount, 2)),
    ADD CONSTRAINT chk_sale_items_treatment_days CHECK (treatment_days BETWEEN 1 AND 3650);
