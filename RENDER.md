# Despliegue de pruebas en Render

El archivo `render.yaml` crea dos recursos:

- `indherco-api`: backend Spring Boot en un Web Service Docker gratuito.
- `indherco-db`: PostgreSQL 16 gratuito, accesible solo desde la red privada de Render.

## Antes de desplegar

1. Subir este repositorio a GitHub, GitLab o Bitbucket.
2. En Render, elegir **New > Blueprint** y conectar el repositorio.
3. Render detectara `render.yaml`.
4. Completar los valores solicitados:
   - `ADMIN_PASSWORD`: clave inicial segura del administrador.
   - `CORS_ALLOWED_ORIGINS`: URL publica exacta del frontend, por ejemplo `https://indherco-web.onrender.com`.
5. Crear los recursos y esperar el primer despliegue.

Flyway ejecuta automaticamente las migraciones `V1` a `V7` sobre la base vacia. La API se puede comprobar en:

```text
https://indherco-api.onrender.com/actuator/health
```

El nombre publico puede variar si `indherco-api` ya esta ocupado.

## Frontend

Crear un **Static Site** adicional en Render con:

```text
Root Directory: frontend
Build Command: npm install && npm run build
Publish Directory: dist
```

Agregar esta variable antes del build:

```text
VITE_API_BASE_URL=https://URL-REAL-DEL-BACKEND.onrender.com/api
```

Despues, actualizar `CORS_ALLOWED_ORIGINS` del backend con la URL real del Static Site y volver a desplegar el backend.

## Limitaciones del plan gratuito

- El Web Service se duerme despues de un periodo sin trafico y el primer acceso puede tardar cerca de un minuto.
- PostgreSQL gratuito tiene 1 GB y expira a los 30 dias. Sirve para pruebas, no para produccion.
- La base Render comienza vacia. Este cambio no copia automaticamente datos existentes desde MySQL.

## Desarrollo local

Crear primero una base PostgreSQL local llamada `indherco`. Luego ejecutar:

```powershell
cd C:\Users\Acer\OneDrive\Documentos\Indherco
.\scripts\run-backend-dev.ps1
```

El script pide el usuario y la clave de PostgreSQL sin guardarlos. El usuario habitual de una instalacion local es `postgres`.
