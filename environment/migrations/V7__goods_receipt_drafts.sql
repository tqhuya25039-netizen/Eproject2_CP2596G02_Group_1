-- Back up the existing database before running this migration once.
-- Existing receipts remain confirmed; newly created receipts will start as drafts.
ALTER TABLE goods_receipts
    ADD COLUMN is_confirmed TINYINT(1) NOT NULL DEFAULT 1 AFTER status;

CREATE TABLE goods_receipt_draft_items (
    draft_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    location_id BIGINT UNSIGNED NOT NULL,
    batch_code VARCHAR(50) NOT NULL,
    expiry_date DATE NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    unit_cost DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (draft_item_id),
    UNIQUE KEY uq_receipt_draft_batch (receipt_id, product_id, location_id, batch_code),
    CONSTRAINT fk_receipt_draft_receipt
        FOREIGN KEY (receipt_id) REFERENCES goods_receipts (receipt_id) ON DELETE CASCADE,
    CONSTRAINT fk_receipt_draft_product
        FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT fk_receipt_draft_location
        FOREIGN KEY (location_id) REFERENCES warehouse_locations (location_id),
    CONSTRAINT chk_receipt_draft_quantity CHECK (quantity > 0),
    CONSTRAINT chk_receipt_draft_cost CHECK (unit_cost >= 0)
) ENGINE=InnoDB;
