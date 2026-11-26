-- Back up the existing database before running this migration once.
-- Do not run this migration automatically from the application.
ALTER TABLE customers
    ADD COLUMN is_blocked TINYINT(1) NOT NULL DEFAULT 0 AFTER is_active,
    ADD COLUMN block_reason VARCHAR(255) NULL AFTER is_blocked,
    ADD CONSTRAINT chk_customers_is_blocked CHECK (is_blocked IN (0, 1)),
    ADD CONSTRAINT chk_customers_block_reason CHECK (
        (is_blocked = 0) OR (block_reason IS NOT NULL AND CHAR_LENGTH(TRIM(block_reason)) > 0)
    );
