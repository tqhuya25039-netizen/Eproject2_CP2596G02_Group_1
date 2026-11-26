-- Back up the existing database before running this migration once.
-- Do not run this migration automatically from the application.
ALTER TABLE sales
    ADD COLUMN redeem_points TINYINT(1) NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN requested_points BIGINT UNSIGNED NULL AFTER redeem_points,
    ADD COLUMN revision BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER requested_points,
    DROP CHECK chk_sales_status,
    ADD CONSTRAINT chk_sales_status CHECK (status IN ('PENDING', 'COMPLETED', 'VOIDED')),
    ADD CONSTRAINT chk_sales_redeem_points CHECK (redeem_points IN (0, 1)),
    ADD CONSTRAINT chk_sales_requested_points CHECK (
        requested_points IS NULL OR requested_points <= 4294967295
    );
