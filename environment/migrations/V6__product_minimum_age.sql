-- Back up the existing database before running this migration once.
-- Do not run this migration automatically from the application.
ALTER TABLE products
    ADD COLUMN minimum_age_years TINYINT UNSIGNED NOT NULL DEFAULT 0 AFTER requires_prescription,
    ADD CONSTRAINT chk_products_minimum_age CHECK (minimum_age_years <= 120);
