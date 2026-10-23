# Patrimonio — Propuesta de cambios de esquema

Documento de análisis y propuesta. **Nada de lo aquí descrito se ha aplicado**:
la base sigue con 20 tablas vacías y la estructura registrada en
`docs/schema-review.sql`.

| Dato | Valor |
|------|-------|
| Motor | PostgreSQL 16.11 (contenedor `patrimonio-postgres`) |
| Base / esquema | `patrimonio` / `public` |
| Tablas | 20 (0 filas) |
| Extensiones | `btree_gist` 1.7 **disponible, no instalada** |
| Migraciones | Ninguna (sin Flyway/Liquibase) |
| Operaciones ejecutadas en esta fase | Solo lectura (`SELECT`, `pg_dump` previo) |
| SQL propuesto | `docs/schema-change-proposal.sql` (no ejecutado) |

---

## 1. Resumen ejecutivo

| # | Cambio | Clasificación | Requiere extensión |
|---|--------|---------------|--------------------|
| 1 | Eliminar `posicion.contrato_id`, su FK y su índice | **Obligatorio** | No |
| 2 | Ampliar `contrato_inversion` (tipo de tasa, referencia, base de días, reinversión, renovación) | Recomendado | No |
| 3 | Sustituir `fecha`/`fecha_inicio`/`fecha_fin` de `historial_tasa` por un `daterange` con exclusión de solapes | Recomendado | **Sí** (`btree_gist`) |
| 4 | Hacer obligatorios los importes de valoración y derivar la ganancia/pérdida | Recomendado | No |
| 5 | Añadir fuente, método, estado y trazabilidad de cotización a las valoraciones | Recomendado | No |
| 6 | Añadir trazabilidad de correcciones y tipo de cambio a `movimiento` | Recomendado | No |
| 7 | Añadir `naturaleza` a `tipo_movimiento` e índice de apoyo a `posicion` | Recomendado | No |
| 8 | Entidad opcional `plan_ppr` y vista `v_contrato_tasa_vigente` | Opcional / pendiente | No |

La relación posición–contrato se resuelve **sin perder los históricos**: tanto
`historial_tasa` como `pago_rendimiento` cuelgan de `contrato_inversion`, y
`valoracion_historica` cuelga de `posicion`; ninguna depende de
`posicion.contrato_id`.

---

## 2. Estado de partida y método

Se analizó el DDL real (`docs/schema-review.sql`, generado con
`pg_dump --schema-only`) y el catálogo de PostgreSQL. Puntos relevantes del
estado actual:

- `posicion.contrato_id` (uuid, NULL) y `contrato_inversion.posicion_id`
  (uuid, NOT NULL) describen la misma relación en sentidos opuestos.
- `historial_tasa` usa `fecha`, `fecha_inicio` y `fecha_fin` sin restricción de
  coherencia entre ellas ni mecanismo que impida tramos superpuestos.
- `contrato_inversion.tasa` es NOT NULL y no distingue tasa fija de variable ni
  base de cálculo.
- `valoracion_historica` admite NULL en sus tres importes y no registra método,
  estado ni la cotización de origen.
- `movimiento` no permite identificar una corrección/reversa ni el tipo de
  cambio aplicado.
- `posicion` no tiene restricción UNIQUE por cuenta e instrumento (correcto para
  lotes, pero sin etiqueta que los distinga).

---

## 3. Decisiones confirmadas y su traducción

| Decisión | Implicación en el modelo |
|----------|--------------------------|
| 1. Varios contratos por posición (renovaciones) | `contrato_inversion.posicion_id` NOT NULL se conserva; se elimina la vía inversa `posicion.contrato_id`. Renovación = nuevo contrato, opcionalmente enlazado con `contrato_origen_id`. |
| 2. Cotizaciones y valoraciones diarias cuando haya datos | Se mantienen dos entidades: `cotizacion` (hecho de mercado) y `valoracion_historica` (valor de la posición). Clave diaria `(posicion_id, fecha)`. |
| 3. Alcance: acciones, ETF, cripto, SOFIPO, plazo y PPR | No requiere tablas nuevas: se cubre con catálogos + `cuenta_financiera` + `instrumento` + `posicion` + `contrato_inversion`. Ver §4.7. |
| 4. Bajas lógicas y `ON DELETE RESTRICT` | Se mantiene RESTRICT en todas las FK; las correcciones se modelan con movimientos de reversa, no con borrados. |

---

## 4. Análisis detallado

### 4.1 Posición ↔ contrato (cambio obligatorio)

**Hallazgo:** dos FK describen la misma relación. Ninguna columna es UNIQUE, por
lo que la base acepta combinaciones incoherentes (la posición A apunta al
contrato C, pero C apunta a la posición B).

**Propuesta:** conservar `contrato_inversion.posicion_id` y eliminar
`posicion.contrato_id`, la FK `fk_posicion_contrato` y el índice
`idx_posicion_contrato`.

Motivos para conservar la vía del contrato:

- Es NOT NULL: todo contrato pertenece a una posición.
- `historial_tasa` y `pago_rendimiento` ya referencian `contrato_inversion(id)`,
  así que los históricos siguen apuntando al contrato correcto sin tocar nada.
- Permite N contratos por posición (renovaciones) sin ambigüedad.
- `valoracion_historica` y `movimiento` referencian la posición, no el contrato;
  no se ven afectados.

**Impacto:** se pierde la posibilidad (hoy sin uso) de que una posición declare
"su" contrato. Si en el futuro hiciera falta el contrato principal, se resuelve
con una vista o con `contrato_origen_id`, no con una segunda FK.

**Verificación propuesta tras aplicar:** `\d posicion` no debe mostrar
`contrato_id`; las FK de `historial_tasa` y `pago_rendimiento` deben seguir
apuntando a `contrato_inversion`.

### 4.2 Historial de tasas

#### Semántica propuesta

| Columna | Semántica |
|---------|-----------|
| `vigencia` (`daterange`) | Intervalo **[inicio, fin)** en el que la tasa aplica. `upper` infinito = tasa vigente sin fecha de término. |
| `tasa` | Tasa efectiva anual aplicable, en **fracción decimal** (`0.0950` = 9.5 %). Se mantiene `numeric(12,8)` y `CHECK (tasa >= 0)`. |
| `fecha_registro` | Momento de captura del registro (auditoría). No es la fecha de vigencia. |
| `motivo` | `INICIAL`, `REVISION`, `RENOVACION`, `AJUSTE` (opcional). |

Se eliminan `fecha`, `fecha_inicio` y `fecha_fin`. `fecha` era ambigua (¿fecha
del cambio o inicio de vigencia?) y las otras dos permitían rangos inválidos.
Con `vigencia` hay una sola fuente de verdad.

#### Por qué [inicio, fin)

Con extremo superior **exclusivo**, dos tramos consecutivos pueden compartir el
día de corte sin solaparse: `[2026-01-01, 2026-02-01)` y
`[2026-02-01, ∞)` son adyacentes, no superpuestos. Con extremo inclusivo, el
mismo día pertenecería a ambos y la restricción los rechazaría.

#### Restricciones propuestas

1. `CHECK (NOT isempty(vigencia) AND lower_inc(vigencia) AND NOT upper_inc(vigencia))`
   → obliga a la forma canónica `[inicio, fin)` y descarta rangos vacíos.
2. `EXCLUDE USING gist (contrato_id WITH =, vigencia WITH &&)`
   → impide dos tramos solapados para el mismo contrato, a nivel de motor y de
   forma atómica (incluido el caso de dos inserciones concurrentes).
3. Se elimina `UNIQUE (contrato_id, fecha)`: con rangos, dos cambios el mismo día
   son legítimos si no se solapan. La exclusión es la restricción correcta.

#### Requisito técnico: `btree_gist`

La restricción `EXCLUDE` mezcla igualdad sobre `uuid` y solapamiento sobre
`daterange`. GiST no tiene operador de igualdad para `uuid`; hace falta
`btree_gist`, que **está disponible (1.7) pero no instalado** y requiere
superusuario. No se instaló ni se creó la restricción.

Alternativas si no se aprueba la extensión:

- **Trigger** `BEFORE INSERT OR UPDATE` que consulte
  `EXISTS (SELECT 1 FROM historial_tasa WHERE contrato_id = NEW.contrato_id AND id <> NEW.id AND vigencia && NEW.vigencia)`.
  Funciona, pero no es atómico frente a concurrencia salvo que se use
  `SERIALIZABLE` o un lock por contrato; además hay que replicarlo en `UPDATE`.
- **Restricción por aplicación**: la más débil; no protege la base.

Recomendación: aprobar `btree_gist` y usar `EXCLUDE`. Es la única opción que
garantiza la invariante en la base sin código adicional.

#### Consulta de tasa vigente

```sql
SELECT tasa
FROM historial_tasa
WHERE contrato_id = :id AND vigencia @> CURRENT_DATE
ORDER BY lower(vigencia) DESC
LIMIT 1;
```

La vista `v_contrato_tasa_vigente` (opcional) encapsula esta consulta.

### 4.3 Contratos de inversión

#### Campos propuestos

| Campo | Tipo | Nulo | Propósito |
|-------|------|------|-----------|
| `posicion_id` | uuid | No | Posición dueña (N contratos por posición). |
| `tipo_tasa` | varchar(10) | No | `FIJA` / `VARIABLE`. |
| `tasa_contractual` | numeric(12,8) | Sí* | Tasa pactada (fija) o tasa inicial (variable). |
| `tasa_referencia` | varchar(50) | Sí | Índice para variable (TIIE, SOFR…). |
| `spread` | numeric(12,8) | Sí | Margen sobre la referencia. |
| `base_calculo_dias` | varchar(10) | Sí | `ACT/360`, `ACT/365`, `30/360`. |
| `modalidad_interes` | varchar(30) | No | `SIMPLE` / `COMPUESTO` (ya existe). |
| `periodicidad_calculo` | varchar(30) | Sí | Frecuencia de devengo. |
| `periodicidad_pago` | varchar(30) | Sí | Frecuencia de pago; `UNICA`/`NULL` = al vencimiento. |
| `fecha_inicio` | date | No | Inicio del contrato (ya existe). |
| `fecha_vencimiento` | date | Sí | Fin; NULL = indefinido. |
| `reinvierte_intereses` | boolean | No | Capitalización de intereses. |
| `renovacion_automatica` | boolean | No | Renovación al vencimiento. |
| `contrato_origen_id` | uuid | Sí | Contrato renovado (auto-FK). |
| `activo` | boolean | No | Baja lógica. |

\* Obligatoria cuando `tipo_tasa = 'FIJA'`; opcional en variable.

#### Evitar redundancias

- **No se almacena `tasa_vigente`**: se deriva del último tramo de
  `historial_tasa` que cubre la fecha. Duplicarla en el contrato garantiza
  desincronización.
- `tasa_contractual` (pactada) e `historial_tasa.tasa` (observada/vigente) tienen
  propósitos distintos; no son redundantes.
- **No se agrega `interes_acumulado`** ni `estado`: son derivables (devengo y
  fechas/`activo`). Si se necesitan por rendimiento, se resuelven con vista o
  tabla de snapshots, no con columnas que haya que mantener a mano.
- `spread` solo aplica a variable; para fija debe ser NULL (lo exige el CHECK
  propuesto para `tasa_referencia`, no para `spread`; ver §9).

#### Devengo, pago y reinversión

- Devengo: `interes = capital × tasa × (dias_devengados / base_dias)`.
  `ACT/360` usa días reales sobre 360 (común en instrumentos de tasa en México),
  `ACT/365` sobre 365, `30/360` con meses de 30 días.
- `modalidad_interes = 'COMPUESTO'` + `reinvierte_intereses = true` → los
  intereses se capitalizan; el CHECK impide marcar reinversión en contratos
  simples.
- `periodicidad_pago = NULL` → no hay pago periódico (se paga al vencimiento o
  se capitaliza). `UNICA` → un solo pago al vencimiento.
- `pago_rendimiento` sigue registrando los pagos efectivos; `historial_tasa` los
  cambios de tasa. No se duplica el calendario de pagos en el contrato.

#### Supuestos abiertos

- `tasa` se guarda como fracción anual, no como porcentaje.
- No se modela retención de impuestos (ISR), día hábil de pago ni ajuste de
  convención de fin de mes; se dejan como decisión pendiente.
- La tasa de un contrato variable se registra ya efectiva (incluyendo spread) en
  `historial_tasa`; `tasa_referencia` y `spread` son informativos.

### 4.4 Valoraciones y cotizaciones

Se mantienen separadas:

| Entidad | Qué representa | Clave natural actual |
|---------|----------------|----------------------|
| `cotizacion` | Precio de mercado de un **instrumento** en una fecha y moneda, con fuente | `(instrumento_id, fecha, moneda_id)` |
| `valoracion_historica` | Valor de una **posición** en una fecha | `(posicion_id, fecha)` |

`valoracion_historica` no debe copiar la cotización completa: referencia la
cotización usada (`cotizacion_id`) y guarda el resultado ya calculado.

#### Campos obligatorios propuestos

- `posicion_id`, `fecha`, `moneda_id` (ya existen).
- `valor_mercado` y `valor_costo`: **NOT NULL** (hoy admiten NULL).
- `metodo`: `COTIZACION_MERCADO`, `ULTIMA_COTIZACION`, `MODELO`, `MANUAL`.
- `estado`: `PRELIMINAR`, `ESTIMADA`, `DEFINITIVA`.
- `cotizacion_id`: NOT NULL cuando `metodo` es de mercado o última cotización.

`fuente` queda opcional, pero recomendada; identifica el origen (proveedor,
ejecución manual, etc.) y es independiente de `cotizacion.fuente` (origen del
precio vs. origen de la valoración).

#### Ganancia/pérdida sin inconsistencias

`ganancia_perdida` pasa a columna generada:

```sql
ganancia_perdida numeric(24,8)
    GENERATED ALWAYS AS (valor_mercado - valor_costo) STORED
```

Ventajas: no se puede desincronizar, no requiere trigger y es consultable e
indexable. Supuesto: ambos importes están en la **misma moneda** (`moneda_id`);
la conversión se hace antes de guardar.

#### Activos sin cotización diaria

No se inventan datos. Opciones, en orden de preferencia:

1. **No crear fila** ese día; la posición conserva su última valoración.
2. **Arrastrar la última cotización** con `metodo = 'ULTIMA_COTIZACION'`,
   `cotizacion_id` apuntando a la cotización real y `estado = 'ESTIMADA'`.
   Queda trazable que el precio no es del día.
3. **Modelo/manual** (`metodo = 'MODELO'` o `'MANUAL'`) con `fuente` que
   documente el criterio.

Nunca rellenar `valor_mercado = 0` como sustituto de "sin dato": un 0 es un
valor financiero válido y distorsiona la ganancia.

#### Decisión pendiente

La clave `(posicion_id, fecha)` asume **una** valoración por día. Si más adelante
se necesitan varias fuentes/métodos por día, habría que ampliar la clave; no se
propone ahora porque la decisión 2 habla de valoración diaria.

### 4.5 Posiciones y movimientos

#### Varias posiciones del mismo instrumento

**Sí se permiten.** Casos reales: lotes con distinto costo, contratos distintos
(PPR, depósitos renovados), cuentas separadas. Por eso **no se propone**
`UNIQUE (cuenta_id, instrumento_id)`:

- impediría lotes y contratos independientes;
- obligaría a fusionar posiciones con costos y fechas distintas;
- complicaría las renovaciones (varios contratos por posición).

Se añade un índice no único `(cuenta_id, instrumento_id)` para agregaciones y un
campo opcional `lote` para distinguirlas. Contrapartida: la aplicación debe
evitar duplicados accidentales; se documenta como validación de negocio, no como
restricción de base.

#### Convenciones de cantidades, importes y comisiones

| Columna | Convención propuesta |
|---------|----------------------|
| `cantidad` | Con signo: `> 0` aumenta unidades, `< 0` las reduce, `0` en movimientos solo de efectivo (dividendo, interés, comisión). |
| `precio` | Precio unitario `>= 0`; `0` en movimientos sin precio (dividendo, ajuste). |
| `importe` | Flujo de caja **desde la perspectiva del portafolio**: `> 0` entra efectivo (venta, dividendo, interés), `< 0` sale (compra, comisión, retiro). |
| `comision` | Costo `>= 0`, siempre separado del importe; no se incluye en `importe` para evitar doble conteo. |
| `moneda_id` | Moneda del importe. Si difiere de la moneda de la posición, se registra `tipo_cambio_aplicado`. |

No se impone `importe = cantidad × precio`: dividendos, intereses, aportaciones,
retiros, splits y ajustes no cumplen esa fórmula. Tampoco se impone
`importe >= 0`, para permitir ajustes y reversas.

#### Movimientos correctivos

- No se actualiza ni se borra un movimiento: se registra otro con signos
  opuestos y `movimiento_origen_id` apuntando al original (`ON DELETE RESTRICT`).
- `ck_movimiento_origen_distinto` impide que un movimiento se referencie a sí
  mismo.
- Bajas lógicas: `activo` en contratos/posiciones; los movimientos históricos no
  se borran.

#### Monedas

`movimiento.moneda_id` puede diferir de `posicion.moneda_id` (operación en otra
divisa). Se propone guardar `tipo_cambio_aplicado` para reproducir el importe
convertido; la fuente oficial sigue en `tipo_cambio`. Si se requiere conservar
importe original y convertido a la vez, haría falta una columna adicional
(decisión pendiente).

### 4.6 PPR

**Con el modelo actual ya se puede representar un PPR:**

```
portafolio → cuenta_financiera (tipo_cuenta = 'PPR')
           → posicion → instrumento (fondo/ETF/acción subyacente)
           → contrato_inversion (si el PPR tiene tasa pactada)
```

- Si el proveedor informa el valor consolidado y no el detalle, el PPR se modela
  como **un instrumento** (tipo `PPR`) con su propia cotización/valoración; no se
  inventan posiciones subyacentes.
- Si se informa el detalle, cada fondo es un `instrumento` y cada participación
  una `posicion` dentro de la misma cuenta PPR.

**Entidad adicional opcional `plan_ppr`:** solo si se quieren reglas propias del
plan (aportación periódica, límite deducible, fecha de retiro estimada). Es 1:1
con `cuenta_financiera` (UNIQUE) y no cambia el resto del modelo. Se entrega como
DDL opcional, no obligatorio.

### 4.7 Cobertura del alcance inicial

| Producto | `clase_activo` (propuesta) | Dónde vive | `contrato_inversion` | Cotización |
|----------|----------------------------|------------|----------------------|------------|
| Acciones | `ACCION` | `instrumento` + `posicion` | No | Diaria de mercado |
| ETF | `FONDO` / `ETF` | `instrumento` + `posicion` | No | Diaria de mercado |
| Criptomonedas | `CRIPTO` | `instrumento` + `posicion` | No | Diaria (24/7, fecha calendario) |
| SOFIPO | `EFECTIVO` | `cuenta_financiera` + `posicion` | Sí: tasa fija, `ACT/360` | No (valor nominal) |
| Depósito a plazo | `RENTA_FIJA` | `cuenta_financiera` + `posicion` | Sí: fija, vencimiento y renovación | No |
| PPR | `FONDO`/`PPR` | `cuenta_financiera` tipo PPR + posiciones | Opcional | Diaria o última disponible |

Los catálogos están vacíos: esta tabla es un mapeo propuesto, **no datos
cargados**.

---

## 5. Clasificación de cambios

### 5.1 Obligatorios

| Cambio | Motivo | Riesgo |
|--------|--------|--------|
| Eliminar `posicion.contrato_id`, `fk_posicion_contrato`, `idx_posicion_contrato` | Relación duplicada e incoherente; decisión confirmada | Bajo: 0 filas y ningún objeto depende de la columna |

### 5.2 Recomendados

| Cambio | Beneficio | Coste |
|--------|-----------|-------|
| Campos de contrato (`tipo_tasa`, `tasa_referencia`, `spread`, `base_calculo_dias`, `reinvierte_intereses`, `renovacion_automatica`, `contrato_origen_id`) | Distingue fija/variable y formaliza devengo, pago y renovación | Más columnas y validaciones en la app |
| `historial_tasa.vigencia` + `EXCLUDE` | Impide solapes a nivel de motor; semántica única | Requiere `btree_gist` y superusuario |
| Valoración: importes NOT NULL y `ganancia_perdida` generada | Elimina NULLs y desincronización | Requiere decidir qué hacer con activos sin dato (se omite la fila) |
| Valoración: `metodo`, `estado`, `fuente`, `cotizacion_id` | Trazabilidad y auditoría del valor | Insert más verboso |
| `movimiento.movimiento_origen_id` y `tipo_cambio_aplicado` | Reversas trazables y operaciones multidivisa | Índice y validación extra |
| `tipo_movimiento.naturaleza` | Reportes sin deducir signos por nombre | Catálogo a poblar |
| Índice `(cuenta_id, instrumento_id)`, `posicion.lote` | Rendimiento y distinción de lotes | Ninguno relevante |

### 5.3 Decisiones pendientes

| # | Tema | Opciones |
|---|------|----------|
| 1 | Instalar `btree_gist` | Aprobar extensión (recomendado) o usar trigger |
| 2 | Impedir solape entre contratos de la misma posición | `EXCLUDE` con rango de `fecha_inicio`/`fecha_vencimiento`; puede haber contratos simultáneos legítimos |
| 3 | Varias valoraciones por día | Mantener 1/día o ampliar clave a `(posicion_id, fecha, metodo)` |
| 4 | Estado del contrato | Derivarlo de fechas/`activo` o crear `estado` (VIGENTE/VENCIDO/CANCELADO) |
| 5 | Día de pago y ajuste hábil | Añadir `dia_pago` y calendario, o dejarlo en la app |
| 6 | Moneda doble en movimientos | Guardar solo convertido + `tipo_cambio_aplicado` o añadir importe original |
| 7 | Comisiones | Columna `comision` o movimiento de tipo comisión; elegir uno para no duplicar |
| 8 | Método de costo | FIFO, promedio ponderado u otro; afecta `precio_promedio` |
| 9 | `plan_ppr` | Crear solo si se gestionan reglas del plan |
| 10 | `fuente` obligatoria en `cotizacion` | Exigirla o permitir NULL |
| 11 | Posiciones cortas | Permitir cantidades negativas (hoy sin CHECK) o restringir |
| 12 | Carga de catálogos | Fase posterior, fuera de esta propuesta |

---

## 6. Contenido del SQL propuesto

`docs/schema-change-proposal.sql` está organizado en secciones equivalentes a
este documento:

| Sección | Contenido | Estado |
|---------|-----------|--------|
| 1 | Elimina `posicion.contrato_id` (FK e índice) | Obligatorio |
| 2 | Amplía `contrato_inversion` y sus CHECK | Recomendado |
| 3 | `historial_tasa` con `vigencia` + `EXCLUDE` | Recomendado (requiere extensión) |
| 4 | `valoracion_historica`: obligatorios, método, estado, generada | Recomendado |
| 5 | `movimiento`, `tipo_movimiento`, `posicion` | Recomendado |
| 6 | `plan_ppr` y `v_contrato_tasa_vigente` | Opcional |

El archivo incluye `BEGIN`/`COMMIT`, comentarios de prerrequisito y consultas de
verificación finales comentadas. **No se ejecutó.**

---

## 7. Impacto y plan de aplicación propuesto

### Preparación

1. Aprobar la instalación de `btree_gist` (o aprobar el trigger alternativo).
2. `pg_dump` completo de seguridad (esquema y datos).
3. Ventana de mantenimiento: `ALTER TABLE` toma bloqueo `ACCESS EXCLUSIVE`,
   aunque con 0 filas es inmediato.
4. Revisar el SQL en una base de prueba antes de producción.

### Orden

1. Sección 1 (obligatoria): quitar la relación duplicada.
2. Sección 2: contratos.
3. Sección 3: historial (crear extensión antes de la restricción `EXCLUDE`).
4. Sección 4: valoraciones.
5. Sección 5: movimientos y posiciones.
6. Sección 6: opcionales, solo si se aprueban.

### Rollback

- Con 0 filas, restaurar el dump previo es suficiente.
- Alternativa: DDL inverso (recrear `posicion.contrato_id`, columnas antiguas de
  `historial_tasa`, quitar columnas nuevas). `ganancia_perdida` generada se puede
  recalcular; no hay pérdida de información.

### Riesgos

| Riesgo | Mitigación |
|--------|------------|
| `SET NOT NULL` falla si hubiera filas con NULL | Hoy 0 filas; validar antes en una copia |
| `EXCLUDE` rechaza datos ya solapados | Hoy 0 filas; si hubiera, depurar antes |
| `DROP COLUMN` elimina datos existentes | Hoy 0 filas; backup previo |
| Dependencia de `btree_gist` | Alternativa con trigger documentada |

---

## 8. Verificación posterior propuesta (no ejecutada)

```sql
-- 1. La columna duplicada ya no existe
\d posicion

-- 2. Restricciones nuevas
SELECT conname, contype
FROM pg_constraint
WHERE conrelid IN ('posicion'::regclass, 'historial_tasa'::regclass,
                   'valoracion_historica'::regclass, 'contrato_inversion'::regclass)
ORDER BY conrelid::regclass::text, conname;

-- 3. La exclusión debe aparecer con contype = 'x'
SELECT conname FROM pg_constraint
WHERE conrelid = 'historial_tasa'::regclass AND contype = 'x';

-- 4. Ganancia derivada (en transacción con ROLLBACK)
BEGIN;
INSERT INTO valoracion_historica
    (posicion_id, fecha, valor_mercado, valor_costo, moneda_id, metodo)
VALUES ('...', CURRENT_DATE, 1100, 1000, '...', 'MANUAL')
RETURNING ganancia_perdida;  -- debe ser 100
ROLLBACK;

-- 5. Solape rechazado (en transacción con ROLLBACK)
BEGIN;
INSERT INTO historial_tasa (contrato_id, tasa, vigencia)
VALUES ('...', 0.10, daterange('2026-01-01', '2026-06-01', '[)')),
       ('...', 0.11, daterange('2026-05-01', '2026-12-31', '[)'));  -- debe fallar
ROLLBACK;

-- 6. Sin cambios inesperados
SELECT count(*) FROM pg_tables WHERE schemaname = 'public';  -- 20
```

---

## 9. Supuestos explícitos

1. Las tasas se guardan como fracción anual (`0.0950` = 9.5 %), no como
   porcentaje.
2. Las fechas de negocio son fechas calendario; no se modelan zonas horarias por
   operación.
3. `valor_mercado` y `valor_costo` están en la misma moneda (`moneda_id`).
4. `valor_costo` es el costo de adquisición más comisiones atribuibles.
5. `importe` es flujo de caja del portafolio y excluye la comisión.
6. El tipo de cambio se registra con la fecha de operación.
7. Una valoración por posición y día.
8. Los contratos pueden renovarse creando uno nuevo; no se reutiliza el mismo
   registro.
9. `plan_ppr` es opcional: el PPR funciona sin él usando `cuenta_financiera`.
10. Los catálogos (monedas, clases, tipos) se cargan en otra fase.

---

## 10. Archivos y confirmación

| Archivo | Estado |
|---------|--------|
| `docs/schema-change-proposal.md` | Este documento (nuevo) |
| `docs/schema-change-proposal.sql` | SQL propuesto (nuevo, **no ejecutado**) |
| `docs/er-diagram.md` | Actualizado con el diagrama objetivo **propuesto** |

Confirmación: no se ejecutó ningún `CREATE`, `ALTER`, `DROP`, `TRUNCATE`,
`INSERT`, `UPDATE` ni `DELETE`. No se instalaron extensiones ni se modificaron el
contenedor, el volumen o la configuración. La base conserva sus 20 tablas vacías
y su estructura original.
