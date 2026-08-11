# Variables de entorno

El backend queda preparado para PostgreSQL y no debe guardar contrasenas ni secretos reales en el repositorio.

## Variables requeridas

- `DB_URL`: URL JDBC de PostgreSQL, por ejemplo `jdbc:postgresql://localhost:5432/indherco`.
- `DB_USERNAME`: usuario de PostgreSQL.
- `DB_PASSWORD`: contrasena de PostgreSQL.
- `JWT_SECRET`: secreto para firmar JWT. Debe ser largo y aleatorio.
- `ADMIN_PASSWORD`: contrasena inicial del administrador si no existe.

## Variables opcionales

- `JWT_EXPIRATION_MINUTES`: expiracion del token. Por defecto `60`.
- `ADMIN_USERNAME`: usuario administrador inicial. Por defecto `admin`.
- `CORS_ALLOWED_ORIGINS`: origenes permitidos separados por coma. Por defecto `http://localhost:5173`.
- `SERVER_PORT`: puerto HTTP. Por defecto `8080`.

## Perfil dev

Para desarrollo local se puede levantar con:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

El perfil `dev` mantiene defaults locales no productivos para `JWT_SECRET` y `ADMIN_PASSWORD`, pero permite sobrescribirlos con variables de entorno.

## Perfil test

El perfil `test` queda preparado para pruebas con Testcontainers PostgreSQL:

```powershell
mvn test -Dspring.profiles.active=test
```

Las pruebas de integracion que usen Testcontainers requieren Docker disponible.

## Nota sobre Flyway

Flyway ya esta agregado y apunta a `classpath:db/migration`. Una base vacia puede crear su esquema aplicando las migraciones existentes.

Para una base ya creada anteriormente por Hibernate, se debe revisar el esquema antes de usar baseline.
