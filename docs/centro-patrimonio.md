# Centro de Patrimonio (Entrega 4)

Módulo `com.patrimonio.consolidacion`: vista consolidada de los activos
registrados, con cobertura explícita, distribución por dimensión, evolución
histórica, resultado realizado, liquidez, eventos y snapshots manuales.

La consolidación **no** es un patrimonio neto completo. Solo incluye las
inversiones registradas a costo y el resultado realizado de los movimientos
confirmados. El efectivo, los activos físicos, los pasivos y el valor de mercado
todavía no tienen fuente en el esquema, por lo que se devuelven como
`NO_DISPONIBLE` con un motivo, nunca como cero.

## Principios financieros

- **Sin conversión de monedas.** Cada importe conserva su divisa. No se suman
  monedas distintas ni se aplican tipos de cambio. Los porcentajes se calculan
  dentro de cada moneda.
- **Costo de adquisición vigente ≠ capital aportado.** El costo actual de las
  posiciones no se presenta como capital aportado histórico.
- **Resultado realizado desde movimientos confirmados.** Se excluyen las
  reversiones y los movimientos originales revertidos para evitar doble conteo.
  Las posiciones cerradas siguen aportando su resultado.
- **Precisión decimal.** Los importes usan `BigDecimal`/`NUMERIC(28,10)` y se
  serializan como cadena con 10 decimales; los porcentajes, con 4.
- **Catálogos extensibles.** `ClaseActivo` y `TipoInstrumento` provienen de los
  catálogos de `inversiones`; no se duplican como enums rígidos.
- **Cobertura antes que cifras.** La respuesta declara qué incluye y qué no
  incluye para evitar interpretar el costo como patrimonio neto.

## Arquitectura

```
com.patrimonio.consolidacion
├── domain.model        Indicador, Cobertura, PeriodoPatrimonial, DimensionDistribucion…
├── application
│   ├── port.in         Casos de uso de consulta y snapshots
│   ├── port.out        PosicionPatrimonioPort, MovimientoPatrimonioPort,
│   │                   CuentaPatrimonioPort, SnapshotPatrimonioRepository,
│   │                   ValuacionPort, EventoPatrimonioPort
│   ├── query/command   ConsultarPatrimonioQuery, ConsultarSnapshotsQuery,
│   │                   CrearSnapshotPatrimonioCommand
│   └── service         ConsultarPatrimonioService, SnapshotPatrimonioService
├── adapter.in.web      Controladores, DTOs, assembler y exception handler
└── adapter.out.persistence  Entidad, repositorio Spring Data, mapper y adaptador
```

Los contratos públicos viven en `com.patrimonio.inversiones.api` y
`com.patrimonio.cuentas.api`. El módulo de consolidación **no** accede a
repositorios ni entidades JPA de otros módulos.

Adaptadores no-op: `ValuacionNoDisponibleAdapter` y
`EventoNoDisponibleAdapter` preparan las interfaces de valuación y eventos sin
inventar datos.

## Endpoints

Requieren sesión y autorización por membresía. Solo se registran si el perfil
`local` o `test` está activo de forma explícita y
`patrimonio.api.consolidacion.expuesta=true`.

### Por portafolio

```http
GET  /api/v1/portafolios/{portafolioId}/patrimonio/centro
GET  /api/v1/portafolios/{portafolioId}/patrimonio/resumen
GET  /api/v1/portafolios/{portafolioId}/patrimonio/distribucion
GET  /api/v1/portafolios/{portafolioId}/patrimonio/evolucion
GET  /api/v1/portafolios/{portafolioId}/patrimonio/rendimientos
GET  /api/v1/portafolios/{portafolioId}/patrimonio/liquidez
GET  /api/v1/portafolios/{portafolioId}/patrimonio/eventos
GET  /api/v1/portafolios/{portafolioId}/patrimonio/snapshots
POST /api/v1/portafolios/{portafolioId}/patrimonio/snapshots
```

### Global (todos los portafolios autorizados)

```http
GET /api/v1/patrimonio/centro
GET /api/v1/patrimonio/resumen
GET /api/v1/patrimonio/distribucion
GET /api/v1/patrimonio/evolucion
GET /api/v1/patrimonio/rendimientos
GET /api/v1/patrimonio/liquidez
GET /api/v1/patrimonio/eventos
```

El endpoint global solo recorre portafolios donde el usuario tiene membresía.
No existe un endpoint global de snapshots: los snapshots son siempre de un
portafolio concreto.

### Parámetros

| Parámetro | Valores | Uso |
|-----------|---------|-----|
| `moneda` | código ISO (`USD`, `EUR`…) | Filtra importes; no convierte |
| `periodo` | `1M`, `3M`, `6M`, `1A`, `TODO` | Series y flujos; por defecto `TODO` |
| `dimension` | `CLASE_ACTIVO`, `TIPO_INSTRUMENTO`, `INSTITUCION`, `CUENTA`, `MONEDA` | Agrupación de la distribución |
| `desde` / `hasta` | fecha ISO (`2026-02-01`) | Rango explícito; prevalece sobre `periodo` |

Un período o dimensión desconocidos responden `400 VALIDACION_ENTRADA`.

### Snapshot manual

```http
POST /api/v1/portafolios/{portafolioId}/patrimonio/snapshots
Content-Type: application/json
X-XSRF-TOKEN: …

{ }
```

El cuerpo es opcional. El snapshot manual solo captura la **fecha actual**: si
se envía `fecha`, debe coincidir con hoy; una fecha pasada responde `422
SNAPSHOT_INVALIDO` porque no se reconstruyen fotografías históricas con datos
presentes. La operación captura una fotografía por moneda con
`capitalInvertido` y `resultadoRealizado` reales. `efectivo`, `pasivos`,
`valorMercado` y `patrimonioNeto` quedan `NULL` mientras no exista fuente.
Responde `201` con la lista de snapshots y la cabecera `Location`. La captura es
**idempotente**: repetirla para la misma combinación
`portafolio + fecha + moneda + fuente` devuelve el snapshot existente en lugar
de duplicarlo.

## Contrato de respuesta

`GET .../centro` devuelve un objeto con:

| Campo | Contenido |
|-------|-----------|
| `meta` | Portafolios incluidos, moneda y período aplicados, fecha de generación, estado global (`PARCIAL` o `NO_DISPONIBLE`), cobertura y limitaciones |
| `resumen` | Patrimonio neto, activos a costo, pasivos, capital invertido, capital aportado, liquidez, resultado realizado y no realizado |
| `distribucion` | Segmentos por moneda para la dimensión elegida |
| `evolucion` | Series de costo, resultado realizado y valor de mercado, con desglose por clase |
| `rendimientos` | Resultado realizado, comisiones, impuestos, intereses, dividendos, resultado no realizado y rendimiento porcentual |
| `liquidez` | Efectivo por cuenta e institución, disponibilidad y capital comprometido |
| `eventos` | Eventos y vencimientos registrados |

Cada indicador tiene `codigo`, `etiqueta`, `estado`
(`DISPONIBLE`/`PARCIAL`/`NO_DISPONIBLE`), `motivo` e `importes[]`. Un indicador
no disponible lleva `importes` vacío y un `motivo`; no se rellena con ceros.

Estados actuales:

| Indicador | Estado |
|-----------|--------|
| Activos registrados a costo | `PARCIAL` |
| Capital invertido a costo | `DISPONIBLE` |
| Resultado realizado acumulado | `DISPONIBLE` |
| Comisiones e impuestos pagados | `DISPONIBLE` |
| Patrimonio neto, pasivos, efectivo, liquidez | `NO_DISPONIBLE` |
| Valor de mercado y resultado no realizado | `NO_DISPONIBLE` |
| Intereses, dividendos y rendimiento porcentual | `NO_DISPONIBLE` |
| Eventos y vencimientos | `NO_DISPONIBLE` (sin registros) |

La evolución incluye los movimientos anteriores a `desde` para calcular el
saldo de apertura, pero no inventa fechas anteriores al primer registro ni
interpola huecos.

## Migración

La tabla `snapshot_patrimonio` es aditiva. En bases nuevas ya está incluida en
`db/esquema.sql`; en bases existentes:

```bash
psql -v ON_ERROR_STOP=1 -f db/esquema-entrega4.sql
```

El script es idempotente, no elimina datos y no modifica posiciones ni
movimientos. Los campos `efectivo`, `pasivos`, `valor_mercado` y
`patrimonio_neto` admiten `NULL`; no se rellenan con el costo de adquisición.

## Exposición

`patrimonio.api.consolidacion.expuesta` vale `false` por defecto y en `prod`.
En `local` y `test` vale `true`, pero el controlador exige además que el perfil
`local` o `test` esté activo de forma explícita. Un arranque sin perfil no
expone los endpoints aunque Spring aplique el perfil por defecto `local`.

En `prod` el arranque falla si se fuerza la propiedad a `true`.

## Pruebas

- Unitarias: `src/test/java/com/patrimonio/consolidacion/`.
- Integración: `src/integrationTest/java/com/patrimonio/consolidacion/`, con
  PostgreSQL 16 en Testcontainers. `PatrimonioApiIT` cubre consolidación,
  aislamiento por membresía, snapshots y exposición condicional.
- Cobertura combinada: `./gradlew jacocoMergedReport`.

La limpieza de tablas en pruebas borra `snapshot_patrimonio` antes de
`portafolio` y `movimiento` antes de `posicion`.
