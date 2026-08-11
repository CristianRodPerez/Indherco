# Seguridad

- Autenticacion con JWT.
- Contrasenas con BCrypt.
- `JWT_SECRET` debe venir desde variable de entorno y tener al menos 32 caracteres.
- CORS se configura con `CORS_ALLOWED_ORIGINS`.
- Roles actuales: `ADMIN_OFICINA`, `OFICINA`, `OPERADOR`.
- Permisos granulares expuestos como authorities de Spring Security.
- Operaciones criticas usan `@PreAuthorize`.
- Login registra intentos fallidos, ultimo acceso y bloqueo temporal.

No se deben registrar ni guardar:

- contrasenas en texto plano;
- tokens JWT completos;
- secretos de configuracion.
