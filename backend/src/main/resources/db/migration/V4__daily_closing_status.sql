ALTER TABLE daily_closings
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'CERRADO',
    ADD COLUMN reopened_by_user_id BIGINT,
    ADD COLUMN reopened_at TIMESTAMP(6),
    ADD COLUMN reopen_reason VARCHAR(500),
    ADD CONSTRAINT fk_daily_closings_reopened_by FOREIGN KEY (reopened_by_user_id) REFERENCES users (id);
