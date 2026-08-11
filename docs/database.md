# Base de datos

## Motor

El backend queda preparado para PostgreSQL 16 o superior.

## Migraciones

Flyway lee migraciones desde:

```text
backend/src/main/resources/db/migration
```

Migracion inicial:

```text
V1__initial_schema.sql
```

## Base nueva

Para una base nueva:

```sql
CREATE DATABASE indherco;
```

Luego levantar el backend con variables de entorno o perfil `dev`. Flyway creara las tablas.

## Base existente creada antes con Hibernate

Si la base ya tiene tablas y datos creados por `ddl-auto=update`, no ejecutes scripts destructivos. Hay dos caminos seguros:

1. Crear una base nueva y migrar datos de forma controlada.
2. Usar baseline de Flyway sobre la base existente, despues de verificar que el esquema coincide con la migracion inicial.

Ejemplo conceptual para baseline:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

Esto no queda activo por defecto para evitar marcar una base incorrecta sin revision.

## Validacion

El backend usa:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

Eso significa que Hibernate valida el esquema, pero no crea ni modifica tablas automaticamente.
