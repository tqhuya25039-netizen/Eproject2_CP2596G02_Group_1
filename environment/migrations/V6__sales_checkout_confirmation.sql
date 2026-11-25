-- Run once after V5 against an existing database after backing it up.
ALTER TABLE sales
    ADD COLUMN requested_points INT UNSIGNED NULL AFTER redeem_points,
    ADD COLUMN revision BIGINT UNSIGNED NOT NULL DEFAULT 0 AFTER requested_points;
