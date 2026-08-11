# Indherco - Control de Produccion y Stock

Aplicacion web/PWA interna para produccion, despacho, consumo, inventario y trazabilidad.

## Carpetas

- `backend`: Spring Boot Java 21 con PostgreSQL, Flyway y repositorios.
- `frontend`: React + Vite + PWA con pantallas iniciales.

## Desarrollo local

Consultar los README de `backend` y `frontend` para ejecutar cada componente.

### Con Docker

Para levantar PostgreSQL, backend y frontend juntos:

```powershell
docker compose up --build -d
```

Abrir `http://localhost:5173`. El usuario inicial es `admin` y, con los valores locales por defecto, la clave es `admin123`.

Para ver el estado y detener el sistema:

```powershell
docker compose ps
docker compose down
```

Los datos de PostgreSQL quedan en el volumen `indherco_postgres_data`. `docker compose down -v` tambien elimina esos datos.

Para desplegar una instancia de pruebas gratuita en Render, consultar [RENDER.md](RENDER.md).

## Despliegue

La guia para una instancia Ubuntu de AWS Lightsail esta en [DEPLOYMENT.md](DEPLOYMENT.md).
