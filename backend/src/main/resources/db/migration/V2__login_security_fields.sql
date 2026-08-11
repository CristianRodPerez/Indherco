ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP(6),
    ADD COLUMN last_login_at TIMESTAMP(6),
    ADD COLUMN last_password_change_at TIMESTAMP(6),
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;
