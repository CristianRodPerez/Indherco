# Auditoria

La auditoria se almacena en `audit_logs`.

Se registran eventos principales:

- login exitoso;
- login fallido;
- creacion/edicion/cambio de estado de usuarios;
- movimientos de stock;
- movimientos de inventario de oficina;
- cierre diario;
- reapertura de cierre;
- anulacion de movimientos.

Cada registro puede incluir usuario, modulo, accion, entidad, IP, user-agent y correlation ID.

Endpoint:

```text
GET /api/audit
```

Requiere permiso `AUDITORIA_VER`.
