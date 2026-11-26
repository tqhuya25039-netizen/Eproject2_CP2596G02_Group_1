-- Historical movement rows must already follow the same sign convention before this is applied.
ALTER TABLE inventory_movements
    ADD CONSTRAINT chk_movements_direction CHECK (
        (movement_type = 'RECEIPT' AND quantity_delta > 0)
        OR (movement_type IN ('SALE', 'DAMAGED', 'EXPIRED') AND quantity_delta < 0)
        OR movement_type = 'ADJUSTMENT'
    );
