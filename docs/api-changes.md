# Cambios de API

Se mantuvieron rutas existentes para compatibilidad con el frontend.

## Nuevos endpoints

```text
GET /api/audit
GET /api/daily-closing
POST /api/daily-closing/{date}/reopen
POST /api/movements/{id}/cancel
```

## Headers

Los endpoints criticos pueden recibir:

```text
Idempotency-Key: valor-unico
X-Correlation-Id: valor-opcional
```

Si `X-Correlation-Id` no viene, el backend genera uno.
