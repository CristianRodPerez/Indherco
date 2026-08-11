# Plan de implementacion backend

## Diagnostico actual

El proyecto `Indherco Postes` ya contiene un backend Spring Boot 3.3.5 con Java 21 y un frontend React/Vite que consume la API existente. El backend esta organizado por modulos funcionales directos bajo `com.indherco.postes`: `auth`, `users`, `products`, `supplies`, `stockmovements`, `dailyclosing`, `dashboard`, `alerts`, `reports` y `officeinventory`.

Actualmente existen entidades JPA, repositorios, servicios, controladores REST, DTOs, seguridad JWT, BCrypt, manejo global de errores basico, Swagger, JaCoCo y pruebas unitarias iniciales para movimientos de stock e inventario de oficina. El stock principal y el inventario de oficina ya usan cantidades enteras y operaciones transaccionales; productos, insumos e inventario de oficina tienen consultas con bloqueo pesimista para operaciones de stock.

La configuracion actual todavia tiene riesgos importantes para un entorno real: `application.yml` contiene credenciales locales, `ddl-auto` esta en `update`, no hay Flyway activo con migraciones, no hay perfiles separados de desarrollo/pruebas, Actuator no esta agregado y el README mantiene referencias antiguas a PostgreSQL. La estructura por capas `domain/application/infrastructure/web` aun no esta aplicada.

## Arquitectura objetivo

El destino es un monolito modular por capas, sin microservicios, manteniendo compatibilidad con el frontend:

- `domain`: entidades, enums, reglas de dominio y excepciones especificas.
- `application`: casos de uso, servicios de aplicacion, commands, queries y validaciones.
- `infrastructure`: persistencia JPA, adaptadores y configuracion tecnica.
- `web`: controladores REST y DTOs de entrada/salida.
- `shared`: configuracion transversal, seguridad, excepciones, auditoria, web y utilidades claramente reutilizables.

La migracion a esta arquitectura debe hacerse gradualmente para no romper rutas ni contratos actuales.

## Funcionalidades ya implementadas

- Login con JWT.
- Usuarios con rol base y permisos simples.
- Productos e insumos.
- Produccion, despacho y consumo con movimientos de stock.
- Historial de movimientos.
- Dashboard oficina.
- Cierre diario basico.
- Alertas simples.
- Reportes basicos.
- Inventario de oficina separado.
- Swagger UI.
- JaCoCo.
- Tests unitarios iniciales.

## Archivos a crear

Fase 1 de planificacion:

- `docs/implementation-plan.md`.

Fase 2 de configuracion MySQL:

- `backend/src/main/resources/application-dev.yml`.
- `backend/src/main/resources/application-test.yml`.
- `.env.example`.
- `docs/environment.md`.

Fases posteriores:

- `backend/src/main/resources/db/migration/V1__initial_schema.sql`.
- `docs/api-changes.md` si cambia algun contrato.
- `docs/architecture.md`.
- `docs/security.md`.
- `docs/database.md`.
- `docs/audit.md`.
- `docs/backup-strategy.md`.
- `docs/implementation-summary.md`.
- `scripts/backup-mysql.ps1`.
- `scripts/restore-mysql.ps1`.

## Archivos a mover

No se moveran archivos en Fase 2. La reorganizacion por capas queda para una fase posterior, porque mover paquetes afecta imports, tests y riesgo de regresiones.

Movimientos futuros sugeridos:

- `auth/security` hacia `auth/infrastructure` o `shared/security` segun responsabilidad.
- controladores hacia subpaquetes `web`.
- DTOs de request/response hacia `web` o `application` segun uso.
- repositorios JPA hacia `infrastructure`.
- entidades y enums hacia `domain`.

## Archivos a modificar

Fase 2:

- `backend/pom.xml`: dependencias MySQL, Flyway, Actuator y Testcontainers MySQL.
- `backend/src/main/resources/application.yml`: variables de entorno, `ddl-auto: validate`, secretos externos, CORS por configuracion, Actuator y Swagger.
- `backend/src/main/resources/application-dev.yml`: valores seguros para desarrollo local.
- `backend/src/main/resources/application-test.yml`: preparacion para pruebas con MySQL/Testcontainers.
- `.env.example`: variables sin secretos reales.
- `docs/environment.md`: guia de variables y perfiles.

## Riesgos

- Cambiar `ddl-auto` de `update` a `validate` puede hacer que el backend no inicie si la base de datos no tiene tablas o si el esquema real no coincide con las entidades.
- Flyway sin migraciones iniciales no crea tablas; la migracion se abordara en la Fase 3.
- Quitar credenciales reales obliga a definir variables de entorno o usar perfil `dev`.
- Reorganizar paquetes en bloque podria romper imports y endpoints; debe hacerse por modulo y con pruebas.
- El frontend espera rutas actuales; se deben conservar contratos salvo cambio documentado.

## Orden de implementacion

1. Crear este plan y dejar claro el alcance.
2. Fase 2: limpiar configuracion MySQL, secretos y perfiles.
3. Validar con `mvn test` y `mvn verify`.
4. Fase 3: agregar migraciones Flyway con baseline documentado.
5. Fase 4: endurecer seguridad, roles/permisos y login.
6. Fase 5: estandarizar errores.
7. Fase 6: completar integridad transaccional, versionado e inventario unificado.
8. Fases 7 a 16: anulaciones, cierre diario avanzado, auditoria, idempotencia, observabilidad, resiliencia, DTO/API, pruebas, documentacion y respaldo.

## Decisiones tecnicas

- Mantener monolito modular.
- Mantener MySQL como unica base objetivo.
- No usar PostgreSQL ni H2 para persistencia principal.
- Mantener endpoints existentes mientras el frontend dependa de ellos.
- Usar variables de entorno para credenciales y secretos.
- Usar perfil `dev` para defaults locales no productivos.
- Usar Flyway desde Fase 3 para gobernar el esquema.
- No usar Lombok.

## Compatibilidad con frontend

Fase 2 no cambia rutas, payloads ni respuestas de API. El frontend puede seguir usando `http://localhost:8080/api`. Si se activa CORS mediante variables, debe incluirse `http://localhost:5173`.

## Criterios de aceptacion

- Existe `docs/implementation-plan.md`.
- `application.yml` no contiene contrasenas reales ni secretos.
- El backend queda configurado para MySQL con variables de entorno.
- `ddl-auto` queda en `validate`.
- Existen perfiles `dev` y `test`.
- Maven incluye Flyway, Actuator y Testcontainers MySQL.
- El proyecto compila y las pruebas unitarias actuales pasan.
- No se modifican contratos del frontend en esta fase.
