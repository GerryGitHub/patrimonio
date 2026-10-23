# Patrimonio — Diagramas ER

Este archivo contiene dos vistas del modelo:

- **§1 Estado aplicado**: lo que existe hoy en PostgreSQL (verificado con
  `pg_dump` y el catálogo).
- **§2 Estado objetivo propuesto**: lo que resultaría si se aprueba
  `docs/schema-change-proposal.sql`. **No está aplicado.**

Para el detalle de FK, restricciones e índices del estado actual, ver
`docs/schema-review.md` y `docs/schema-review.sql`.

---

## 1. Estado aplicado (2026-10-23)

20 tablas · 28 FK · 20 PK · 12 UNIQUE · 18 CHECK · 62 índices · 0 filas.

```mermaid
erDiagram
    usuario ||--o{ membresia : "usuario_id"
    portafolio ||--o{ membresia : "portafolio_id"

    moneda ||--o{ portafolio : "moneda_base_id"
    moneda ||--o{ cuenta_financiera : "moneda_id"
    moneda ||--o{ instrumento : "moneda_id"
    moneda ||--o{ posicion : "moneda_id"
    moneda ||--o{ movimiento : "moneda_id"
    moneda ||--o{ pago_rendimiento : "moneda_id"
    moneda ||--o{ cotizacion : "moneda_id"
    moneda ||--o{ valoracion_historica : "moneda_id"
    moneda ||--o{ tipo_cambio : "moneda_origen_id"
    moneda ||--o{ tipo_cambio : "moneda_destino_id"

    clase_activo ||--o{ tipo_instrumento : "clase_activo_id"
    tipo_instrumento ||--o{ instrumento : "tipo_instrumento_id"
    instrumento ||--o{ identificador_instrumento : "instrumento_id"
    instrumento ||--o{ posicion : "instrumento_id"
    instrumento ||--o{ cotizacion : "instrumento_id"

    institucion ||--o{ cuenta_financiera : "institucion_id"
    tipo_cuenta ||--o{ cuenta_financiera : "tipo_cuenta_id"
    portafolio ||--o{ cuenta_financiera : "portafolio_id"

    cuenta_financiera ||--o{ posicion : "cuenta_id"
    posicion ||--o{ movimiento : "posicion_id"
    tipo_movimiento ||--o{ movimiento : "tipo_movimiento_id"

    posicion ||--o{ contrato_inversion : "posicion_id"
    contrato_inversion ||--o{ posicion : "contrato_id (NULL)"

    contrato_inversion ||--o{ historial_tasa : "contrato_id"
    contrato_inversion ||--o{ pago_rendimiento : "contrato_id"

    posicion ||--o{ valoracion_historica : "posicion_id"
```

**Relación duplicada:** `posicion.contrato_id` y `contrato_inversion.posicion_id`
representan la misma relación en sentidos opuestos. Ver §2.1.

---

## 2. Estado objetivo propuesto (NO aplicado)

Cambios respecto a §1:

- Se elimina `posicion.contrato_id` (y su FK e índice).
- `contrato_inversion` gana tipo de tasa, referencia, spread, base de cálculo,
  reinversión, renovación y auto-referencia de renovación.
- `historial_tasa` usa un rango `vigencia [inicio, fin)` con restricción de
  exclusión (requiere `btree_gist`, aún no instalada).
- `valoracion_historica` exige importes, deriva la ganancia y registra método,
  estado y cotización de origen.
- `movimiento` gana trazabilidad de reversas y tipo de cambio.
- `plan_ppr` es opcional.

```mermaid
erDiagram
    usuario ||--o{ membresia : "usuario_id"
    portafolio ||--o{ membresia : "portafolio_id"

    moneda ||--o{ portafolio : "moneda_base_id"
    moneda ||--o{ cuenta_financiera : "moneda_id"
    moneda ||--o{ instrumento : "moneda_id"
    moneda ||--o{ posicion : "moneda_id"
    moneda ||--o{ movimiento : "moneda_id"
    moneda ||--o{ pago_rendimiento : "moneda_id"
    moneda ||--o{ cotizacion : "moneda_id"
    moneda ||--o{ valoracion_historica : "moneda_id"
    moneda ||--o{ tipo_cambio : "moneda_origen_id"
    moneda ||--o{ tipo_cambio : "moneda_destino_id"

    clase_activo ||--o{ tipo_instrumento : "clase_activo_id"
    tipo_instrumento ||--o{ instrumento : "tipo_instrumento_id"
    instrumento ||--o{ identificador_instrumento : "instrumento_id"
    instrumento ||--o{ posicion : "instrumento_id"
    instrumento ||--o{ cotizacion : "instrumento_id"

    institucion ||--o{ cuenta_financiera : "institucion_id"
    tipo_cuenta ||--o{ cuenta_financiera : "tipo_cuenta_id"
    portafolio ||--o{ cuenta_financiera : "portafolio_id"
    cuenta_financiera ||--o| plan_ppr : "opcional 1:1"

    cuenta_financiera ||--o{ posicion : "cuenta_id"
    posicion ||--o{ movimiento : "posicion_id"
    tipo_movimiento ||--o{ movimiento : "tipo_movimiento_id"
    movimiento |o--o{ movimiento : "movimiento_origen_id (NULL)"

    posicion ||--o{ contrato_inversion : "posicion_id"
    contrato_inversion |o--o{ contrato_inversion : "contrato_origen_id (NULL)"
    contrato_inversion ||--o{ historial_tasa : "contrato_id"
    contrato_inversion ||--o{ pago_rendimiento : "contrato_id"

    posicion ||--o{ valoracion_historica : "posicion_id"
    cotizacion |o--o{ valoracion_historica : "cotizacion_id (NULL)"

    posicion {
        uuid id PK
        uuid cuenta_id FK
        uuid instrumento_id FK
        uuid moneda_id FK
        numeric cantidad
        numeric precio_promedio
        varchar lote "nuevo, opcional"
        boolean activo
    }

    contrato_inversion {
        uuid id PK
        uuid posicion_id FK
        varchar tipo_tasa "FIJA | VARIABLE"
        numeric tasa_contractual
        varchar tasa_referencia
        numeric spread
        varchar base_calculo_dias
        varchar modalidad_interes
        varchar periodicidad_calculo
        varchar periodicidad_pago
        boolean reinvierte_intereses
        boolean renovacion_automatica
        uuid contrato_origen_id FK
        date fecha_inicio
        date fecha_vencimiento
        boolean activo
    }

    historial_tasa {
        uuid id PK
        uuid contrato_id FK
        numeric tasa
        daterange vigencia "nuevo [inicio, fin)"
        timestamptz fecha_registro
        varchar motivo
    }

    valoracion_historica {
        uuid id PK
        uuid posicion_id FK
        date fecha
        numeric valor_mercado "NOT NULL"
        numeric valor_costo "NOT NULL"
        numeric ganancia_perdida "generada"
        uuid cotizacion_id FK "NULL"
        varchar metodo
        varchar estado
        varchar fuente
        uuid moneda_id FK
    }

    plan_ppr {
        uuid id PK
        uuid cuenta_financiera_id FK "UNIQUE"
        numeric aportacion_periodica
        varchar periodicidad_aportacion
        numeric limite_deducible_anual
        date fecha_retiro_estimada
        boolean activo
    }
```

### 2.1 Diferencias concretas

| Elemento | Estado aplicado | Estado propuesto |
|----------|-----------------|------------------|
| `posicion.contrato_id` | Existe (NULL), FK e índice | **Eliminado** |
| FK posición ↔ contrato | 2 vías (`posicion.contrato_id`, `contrato_inversion.posicion_id`) | 1 vía (`contrato_inversion.posicion_id`) |
| Contratos por posición | N (sin restricción de coherencia) | N, con auto-referencia de renovación |
| `contrato_inversion.tasa` | `tasa` NOT NULL | `tasa_contractual` (NULL si variable), + `tipo_tasa`, `tasa_referencia`, `spread` |
| Base de cálculo | No existe | `base_calculo_dias` (`ACT/360`, `ACT/365`, `30/360`) |
| Devengo/pago | Solo periodicidades | + `reinvierte_intereses`, CHECK de reinversión |
| `historial_tasa` | `fecha`, `fecha_inicio`, `fecha_fin` sin coherencia | `vigencia daterange` + `fecha_registro` + `motivo` + `EXCLUDE` |
| Solapes de tasa | Permitidos | Rechazados por el motor |
| `valoracion_historica` | 3 importes NULL, sin metadatos | Importes NOT NULL, `ganancia_perdida` generada, `metodo`, `estado`, `fuente`, `cotizacion_id` |
| `valoracion_historica` ↔ `cotizacion` | Sin vínculo | FK opcional `cotizacion_id` |
| `movimiento` | Sin trazabilidad de correcciones | + `movimiento_origen_id`, `tipo_cambio_aplicado` |
| `tipo_movimiento` | Sin naturaleza | + `naturaleza` (`ENTRADA`/`SALIDA`/`NEUTRA`/`AJUSTE`) |
| `posicion` | Sin etiqueta de lote | + `lote` (opcional) |
| PPR | Solo como cuenta/instrumento | Igual + `plan_ppr` opcional |
| Vistas | Ninguna | `v_contrato_tasa_vigente` (opcional) |

Si se aplicara todo, el número de FK sería 31: 28 − 1 (`posicion.contrato_id`) +
4 nuevas (`valoracion_historica.cotizacion_id`, `contrato_inversion.contrato_origen_id`,
`movimiento.movimiento_origen_id`, `plan_ppr.cuenta_financiera_id`).

**Nada de esto está aplicado.** El estado real sigue siendo el de §1.
