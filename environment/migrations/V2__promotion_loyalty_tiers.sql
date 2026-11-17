-- Run once against an existing database after backing it up.
-- Do not run this migration automatically from the application.
ALTER TABLE promotions
    ADD COLUMN points_redemption_starts_at DATETIME NULL AFTER starts_at,
    ADD COLUMN points_earning_ends_at DATETIME NULL AFTER points_redemption_starts_at,
    ADD COLUMN redemption_vnd_per_point DECIMAL(20, 8) NULL AFTER ends_at;

UPDATE promotions p
SET p.points_redemption_starts_at = p.starts_at,
    p.points_earning_ends_at = p.ends_at,
    p.redemption_vnd_per_point = COALESCE(
        (SELECT lp.currency_per_point
         FROM loyalty_programs lp
         ORDER BY lp.loyalty_program_id
         LIMIT 1),
        1.00000000
    );

ALTER TABLE promotions
    MODIFY COLUMN points_redemption_starts_at DATETIME NOT NULL,
    MODIFY COLUMN points_earning_ends_at DATETIME NOT NULL,
    MODIFY COLUMN redemption_vnd_per_point DECIMAL(20, 8) NOT NULL,
    DROP CHECK chk_promotions_dates,
    ADD CONSTRAINT chk_promotions_point_dates CHECK (
        starts_at <= points_redemption_starts_at
        AND points_redemption_starts_at <= points_earning_ends_at
        AND points_earning_ends_at <= ends_at
    ),
    ADD CONSTRAINT chk_promotions_redemption_rate CHECK (redemption_vnd_per_point > 0);

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

INSERT INTO promotion_point_tiers
    (promotion_id, minimum_invoice_total_vnd, points_rate_percent)
SELECT p.promotion_id,
       0,
       GREATEST(COALESCE(lp.points_per_1000_currency, 1) * p.points_multiplier / 10, 0.00000001)
FROM promotions p
LEFT JOIN loyalty_programs lp ON lp.loyalty_program_id = (
    SELECT MIN(loyalty_program_id) FROM loyalty_programs
);
