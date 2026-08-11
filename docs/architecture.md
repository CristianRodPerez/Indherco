# Arquitectura

El backend es un monolito modular Spring Boot. La arquitectura objetivo es separar cada modulo en `domain`, `application`, `infrastructure` y `web`, manteniendo contratos actuales.

Estado actual:

- Modulos funcionales existentes: auth, users, products, supplies, stockmovements, dailyclosing, alerts, reports, officeinventory, audit e idempotency.
- Controladores REST no exponen entidades JPA directamente.
- Servicios concentran reglas de negocio.
- Repositorios quedan fuera de controladores.
- Puertos de integracion futura: correo, generacion de reportes y almacenamiento externo.

La reorganizacion fisica por capas queda pendiente para hacerse modulo por modulo, evitando romper imports y rutas.
