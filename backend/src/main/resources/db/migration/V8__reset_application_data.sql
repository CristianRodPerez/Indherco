-- Limpieza unica solicitada para iniciar la operacion real sin datos de prueba.
-- DataInitializer recrea solamente el usuario administrador al terminar Flyway.
TRUNCATE TABLE
    daily_alert_dismissals,
    idempotency_records,
    audit_logs,
    office_inventory_movements,
    office_inventory_items,
    alerts,
    daily_closings,
    stock_movements,
    supplies,
    products,
    users
RESTART IDENTITY CASCADE;
