# Indherco Postes - Frontend

Base React + Vite + PWA para el MVP.

## Requisitos

- Node.js 20+
- npm o pnpm

## Comandos

```bash
npm install
npm run dev
npm run build
```

Durante desarrollo, Vite envia `/api` a:

```text
http://localhost:8080/api
```

Puede cambiarse con:

```text
VITE_API_BASE_URL
```

El build productivo usa `/api` mediante `.env.production`. Nginx debe enviar esa ruta al backend Spring Boot.
