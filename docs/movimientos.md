# Motor de movimientos de inversión (Entrega 3)

Este documento describe el motor de movimientos implementado en la Entrega 3.
Los **movimientos confirmados son la fuente de verdad** y la posición es un
estado materializado que se actualiza en la misma transacción.

## Alcance

- Registro de aperturas, compras, ventas y ajustes positivos/negativos.
- Cálculo de cantidad, costo total, costo promedio y resultado realizado.
- Historial paginado y filtrable por posición.
- Reversión mediante movimientos compensatorios.
- Reconstrucción y verificación de la posición desde el historial.
- Migración idempotente de posiciones existentes a movimientos `APERTURA`.
- Dashboard con cantidad mantenida, costo de adquisición, costo promedio y
  resultado realizado.

No incluye valuación de mercado, rendimiento no realizado, transferencias entre
cuentas ni contratos de inversión a plazo.

## Tipos de movimiento

| Tipo | Uso | Efecto |
| --- | --- | --- |
| `APERTURA` | Saldo inicial de una posición. | Incrementa cantidad y costo. |
| `COMPRA` | Adquisición adicional. | Incrementa cantidad y costo; los gastos atribuibles se suman al costo. |
| `VENTA` | Reducción de la tenencia. | Reduce cantidad y costo con el promedio ponderado; registra costo aplicado y resultado realizado. |
| `AJUSTE_POSITIVO` | Corrección justificada que agrega tenencia. | Incrementa cantidad y el costo total explícito indicado. |
| `AJUSTE_NEGATIVO` | Corrección justificada que retira tenencia. | Reduce cantidad y el costo total explícito indicado. |
| `REVERSION` | Compensatorio de otro movimiento. | Aplica los deltas inversos al original. No se opera directamente ni se revierte. |

Reglas principales:

- La cantidad de todo movimiento operable debe ser mayor que cero.
- Una venta o un ajuste negativo no puede superar la cantidad disponible.
- Una posición inactiva no admite movimientos.
- El ajuste negativo que deja la cantidad en cero debe retirar todo el costo.
- Un movimiento confirmado es inmutable. Las correcciones se registran con un
  compensatorio vinculado por `movimientoOrigenId`.
- Un movimiento ya revertido no se revierte de nuevo.
- La moneda se deriva del instrumento; el cliente no la envía.
- Los importes derivados (bruto, neto, costo aplicado y resultado) los calcula
  el backend.

## Cálculos y escalas

- Cantidad, importes y costo total: 10 decimales.
- Precio unitario: 8 decimales.
- Costo unitario derivado: 8 decimales.
- Redondeo: `HALF_UP`.
- El costo total es la magnitud autoritativa; el costo unitario es una vista
  calculada como `costo total / cantidad`.
- Compra: `importe neto = importe bruto + comisiones + impuestos`; ese neto se
  suma al costo total.
- Venta: `importe neto = importe bruto - comisiones - impuestos`; el costo
  aplicado usa el costo promedio vigente y
  `resultado realizado = importe neto - costo aplicado`.
- La posición conserva `costo_total`, `precio_promedio`, `resultado_realizado`,
  `fecha_ultimo_movimiento` y `version`.

## Persistencia y migración

La tabla `movimiento` almacena el movimiento confirmado y los deltas firmados
`efecto_cantidad` y `efecto_costo`. La tabla `posicion` conserva el estado
materializado.

Restricciones relevantes:

- `ck_movimiento_cantidad`: cantidad mayor que cero.
- `uq_movimiento_reversion`: un movimiento original solo puede tener una
  reversión.
- `uq_movimiento_idempotencia`: una clave de idempotencia no se repite dentro del
  portafolio.
- Índices por posición/fecha y portafolio/fecha para el historial.

Para una base existente, detener la aplicación y ejecutar:

```bash
psql -v ON_ERROR_STOP=1 -f db/esquema-entrega3.sql
psql -v ON_ERROR_STOP=1 -f db/datos-iniciales.sql
```

`db/esquema-entrega3.sql` es aditivo e idempotente. Convierte cada posición sin
movimientos en un movimiento `APERTURA` que conserva su UUID, cantidad, costo y
fecha de apertura. Una segunda ejecución no duplica la apertura ni reescribe el
estado. `db/esquema.sql` ya contiene el esquema completo para instalaciones
nuevas. No se usa Flyway ni Liquibase.

Las posiciones que no pueden convertirse de forma coherente (por ejemplo, costo
total que no coincide con cantidad × costo unitario más allá de la tolerancia)
se documentan en las consultas de verificación del script y deben revisarse
antes de habilitar movimientos.

## API

Los endpoints solo se registran con un perfil `local` o `test` activo de forma
explícita y `patrimonio.api.inversiones.expuesta=true`.

```http
GET   /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/movimientos
POST  /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/movimientos
GET   /api/v1/portafolios/{portafolioId}/movimientos/{movimientoId}
POST  /api/v1/portafolios/{portafolioId}/movimientos/{movimientoId}/reversion
GET   /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/resumen
POST  /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/reconstruccion
```

El historial acepta `tipo`, `desde`, `hasta`, `pagina`, `tamano` y `orden`
(`asc`/`desc`). La respuesta incluye `items`, `pagina`, `tamano`, `total` y
`totalPaginas`.

El alta acepta:

```json
{
  "tipo": "COMPRA",
  "fechaOperacion": "2026-01-15",
  "fechaLiquidacion": "2026-01-17",
  "cantidad": "10",
  "precioUnitario": "25.50",
  "comisiones": "1.20",
  "impuestos": "0.80",
  "tipoCambio": "1",
  "observaciones": "Compra inicial",
  "claveIdempotencia": "uuid-o-texto"
}
```

Los ajustes usan `costoTotalAjuste` en lugar de precio. La clave de idempotencia
también se acepta en la cabecera `Idempotency-Key`; la del cuerpo tiene
precedencia. Un reintento con los mismos datos devuelve `201` con el movimiento
original; con datos distintos devuelve `409 IDEMPOTENCIA_CONFLICTO`.

La reversión acepta `fechaOperacion`, `observaciones` y `claveIdempotencia`.

Códigos de error estables:

| Código | HTTP | Significado |
| --- | --- | --- |
| `MOVIMIENTO_INVALIDO` | 422 | Regla de negocio incumplida. |
| `MOVIMIENTO_NO_REVERSIBLE` | 409 | El movimiento no admite reversión. |
| `MOVIMIENTO_YA_REVERTIDO` | 409 | Ya existe un compensatorio. |
| `IDEMPOTENCIA_CONFLICTO` | 409 | La clave se usó con otros datos. |
| `CONFLICTO_CONCURRENCIA` | 409 | Otro cambio simultáneo. |
| `POSICION_CON_HISTORIAL` | 422 | Intento de editar cantidad/costo con historial. |

## Concurrencia e idempotencia

- La posición se bloquea de forma pesimista antes de calcular y aplicar un
  movimiento. El movimiento y la posición se guardan en una única transacción.
- La clave de idempotencia se resuelve antes de escribir. En concurrencia real,
  el segundo intento puede recibir `409 CONFLICTO_CONCURRENCIA`; el cliente debe
  consultar el historial antes de reintentar.
- La posición conserva una versión para detectar modificaciones inesperadas.

## Reconstrucción

`POST .../reconstruccion` recorre el historial confirmado y compara cantidad,
costo y resultado con el estado materializado.

- Con `{"reparar": false}` solo informa `coincide` y `diferencias`.
- Con `{"reparar": true}` corrige únicamente el estado materializado.
- Los movimientos nunca se modifican ni se eliminan.

## Edición de posiciones

- Una posición sin movimientos conserva la edición heredada de cantidad, costo
  y fecha de apertura.
- Una posición con historial rechaza cambios de cantidad, costo o fecha de
  apertura con `POSICION_CON_HISTORIAL`. Las observaciones siguen siendo
  editables.
- Crear una posición genera automáticamente su movimiento `APERTURA`.

## Frontend y dashboard

- El detalle de posición muestra cuatro métricas separadas: cantidad mantenida,
  costo de adquisición, costo promedio y resultado realizado.
- El historial permite filtrar por tipo, rango de fechas y orden, y pagina los
  resultados.
- El formulario de movimiento calcula una vista previa del importe bruto, neto,
  costo aplicado y resultado; el backend vuelve a calcular los importes.
- Cada movimiento se puede consultar en un diálogo de detalle.
- La reversión exige confirmación y explica que el original no se modifica.
- La verificación de saldos informa diferencias y permite reparar solo el estado
  materializado.
- El dashboard presenta el resultado realizado por moneda y mantiene el costo
  como costo de adquisición, nunca como valor de mercado.

## Pruebas

- Backend: pruebas unitarias del motor, estrategias, `Movimiento`, `Posicion` y
  validadores; pruebas de integración con PostgreSQL/Testcontainers para alta,
  venta, ajustes, reversión, idempotencia, concurrencia, filtros, aislamiento,
  migración y reconstrucción.
- Frontend: pruebas de clientes de API, validaciones, cálculos visuales, tabla de
  historial, formulario, resultado por moneda y combinación de resúmenes; además
  de `typecheck` y `build`.

## Limitaciones

- No hay valuación de mercado ni rendimiento no realizado.
- No hay transferencias entre cuentas ni entre portafolios.
- No se modelan contratos de inversión a plazo en este motor.
- La clasificación funcional de tipos de cuenta sigue pendiente; las reglas de
  compatibilidad usan el nombre del tipo.
