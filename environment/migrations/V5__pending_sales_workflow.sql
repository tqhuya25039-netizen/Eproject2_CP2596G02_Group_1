-- Back up the existing database before running this migration once.
-- Do not run this migration automatically from the application.
ALTER TABLE sales
    ADD COLUMN redeem_points BOOLEAN NOT NULL DEFAULT FALSE AFTER status,
    DROP CHECK chk_sales_status,
    ADD CONSTRAINT chk_sales_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'VOIDED'));
