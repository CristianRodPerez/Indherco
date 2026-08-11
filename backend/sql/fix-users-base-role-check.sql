ALTER TABLE users DROP CONSTRAINT IF EXISTS users_base_role_check;

ALTER TABLE users
ADD CONSTRAINT users_base_role_check
CHECK (base_role IN ('ADMIN_OFICINA', 'OFICINA', 'OPERADOR'));
