ALTER TABLE stock_movements
    ADD COLUMN cancelled_by_user_id BIGINT,
    ADD COLUMN cancelled_at TIMESTAMP(6),
    ADD COLUMN cancellation_reason VARCHAR(500),
    ADD COLUMN reversal_movement_id BIGINT,
    ADD CONSTRAINT fk_stock_movements_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES users (id);
