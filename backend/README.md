# Indherco Postes - Backend

Backend Spring Boot para el MVP de control de produccion, despacho, consumo, stock, usuarios, cierre diario, reportes e inventario de oficina.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL 16+
- Docker con contenedores Linux para las pruebas de integracion con Testcontainers

## Configuracion

El backend usa PostgreSQL y variables de entorno. No se deben guardar contrasenas reales ni secretos en el repositorio.

Variables principales:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
ADMIN_USERNAME
ADMIN_PASSWORD
CORS_ALLOWED_ORIGINS
SERVER_PORT
```

Hay un ejemplo en `.env.example` y una guia completa en `docs/environment.md`.

## Desarrollo local

Para levantar con defaults locales no productivos:

```powershell
mvn spring-boot:run
```

El plugin de Maven levanta el perfil `dev` por defecto para desarrollo local. Si usas el perfil principal fuera de Maven, debes definir al menos `DB_PASSWORD`, `JWT_SECRET` y `ADMIN_PASSWORD`.

Para sobrescribir la conexion local en PowerShell:

```powershell
$env:DB_USERNAME="tu_usuario"
$env:DB_PASSWORD="tu_password"
$env:DB_URL="jdbc:postgresql://localhost:5432/indherco"
mvn spring-boot:run
```

Tambien puedes usar el script que pide las credenciales sin guardarlas:

```powershell
cd C:\Users\Acer\OneDrive\Documentos\Indherco
.\scripts\run-backend-dev.ps1
```

## Validacion

```powershell
mvn clean test
mvn clean verify
```

- `mvn clean test` ejecuta las pruebas unitarias y compila la suite de integracion.
- `mvn clean verify` ejecuta tambien `CriticalBusinessFlowsIT` contra PostgreSQL 16 real mediante Testcontainers.
- Docker debe estar iniciado antes de usar `verify`. En Windows, Docker Desktop requiere WSL 2 o el motor de contenedores Linux habilitado.
- El reporte JaCoCo queda en `target/site/jacoco/index.html` despues de `verify`.

Los tests de integracion limpian automaticamente sus datos antes de cada escenario. No se conectan a la base de desarrollo ni usan H2.

## Migraciones

Flyway lee las migraciones desde `src/main/resources/db/migration` y prepara tambien la base efimera de los tests de integracion.
