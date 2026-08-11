# Indherco MVP - Notas tecnicas

## Cantidades enteras

Todas las cantidades y stocks se manejan como enteros (`Integer` en backend). La interfaz usa inputs `step=1` para evitar decimales.

## Regla central de stock

No se debe modificar el stock directamente en operacion diaria. Todo cambio operativo debe registrarse mediante un movimiento:

- Produccion aumenta stock de producto.
- Despacho disminuye stock de producto.
- Consumo disminuye stock de insumo.
- Inventario oficina usa movimientos `ENTRADA` y `CONSUMO`.

## Concurrencia

Las actualizaciones de stock usan bloqueo pesimista (`PESSIMISTIC_WRITE`) al leer el item afectado. Esto evita que dos peticiones simultaneas descuenten o sumen desde el mismo stock anterior.

## Swagger

Con el backend corriendo:

```text
http://localhost:8080/swagger-ui.html
```

## Tests y cobertura

Ejecutar:

```powershell
mvn test
```

JaCoCo genera reporte en:

```text
backend/target/site/jacoco/index.html
```

## Migracion MySQL para cantidades enteras

Si ya existian tablas con decimales, ejecutar:

```text
backend/sql/mysql-integer-quantities.sql
```
