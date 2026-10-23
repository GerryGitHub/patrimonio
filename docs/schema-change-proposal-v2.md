# Patrimonio — Propuesta de cambios de esquema (v2)

Segunda versión de la propuesta. **No se ha ejecutado nada**: la base sigue con
20 tablas vacías, sin extensiones adicionales (solo `plpgsql`) y con la
estructura original.

| Dato | Valor |
|------|-------|
| Motor | PostgreSQL 16.11 |
| Base / esquema | `patrimonio` / `public` |
| Tablas / filas | 20 / 0 |
| `btree_gist` | Disponible 1.7, **no instalada**, marcada `trusted` |
| SQL propuesto | `docs/schema-change-proposal-v2.sql` (no ejecutado) |
| Documento anterior | `docs/schema-change-proposal.md` (v1) |

---

## 1. Correcciones solicitadas y cómo se resolvieron

| # | Punto | Resolución en v2 |
|---|-------|------------------|
| 1 | Dependencia de `btree_gist` | Dos alternativas explícitas: N3-A con extensión (activa por defecto) y N3-B con trigger + advisory lock (comentada). Se puede aplicar una sola. |
| 2 | Semántica de vigencia | `vigencia daterange` con regla **[inicio, fin)**, fin **exclusivo**, inicio finito obligatorio y CHECK de forma canónica. Regla documentada para convertir “hasta el día X inclusive”. |
| 3 | Obligatorios de `valoracion_historica` | Se mantienen `valor_mercado`/`valor_costo` NOT NULL, pero se amplían métodos para cubrir devengo (plazo/SOFIPO) y proveedor (PPR). Se documenta el significado por clase de activo. |
| 4 | Movimientos multidivisa | Se guardan **importe original + importe convertido + tasa + fecha + fuente**, con CHECK de consistencia. |
| 5 | Renovaciones entre posiciones | FK compuesta `(contrato_origen_id, posicion_id) → (id, posicion_id)`: el contrato origen debe ser de la misma posición. |
| 6 | Reinversión obligaba a interés compuesto | Se elimina ese CHECK. Reinversión y modalidad de interés son conceptos independientes. |
| 7 | Separar necesario de opcional | Bloques `N1–N5` (necesarios) y bloque opcional comentado. `plan_ppr` y `v_contrato_tasa_vigente` **solo** en el bloque opcional. |
| 8 | Una sola relación posición–contrato | Se conserva solo `contrato_inversion.posicion_id` (N:1, varios contratos por posición) y se elimina `posicion.contrato_id`. |

---

## 2. Alternativas para la restricción de solape (punto 1)

Se requiere impedir tramos de tasa solapados para un mismo contrato. Hay dos
caminos y **solo debe aplicarse uno**.

### 2.1 Alternativa A — `EXCLUDE` con `btree_gist` (recomendada)

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE historial_tasa
    ADD CONSTRAINT ex_historial_tasa_sin_solape
        EXCLUDE USING gist (contrato_id WITH =, vigencia WITH &&);
```

**Por qué hace falta la extensión:** la restricción mezcla igualdad sobre `uuid`
y solapamiento (`&&`) sobre `daterange`. GiST no incluye operadores de igualdad
para `uuid`; `btree_gist` los aporta para poder indexar `contrato_id` con GiST.

| Ventaja | Inconveniente |
|---------|---------------|
| Garantía a nivel de motor, atómica incluso con inserciones concurrentes | Requiere `CREATE EXTENSION` |
| Sin código que mantener | La extensión debe existir también en la base restaurada (pg_dump la incluye) |
| El índice GiST sirve además para consultas por rango | Índice algo mayor que un btree |
| Error estándar `23P01` (`exclusion_violation`) | Si el proveedor no permite extensiones, no es viable |

Estado en este servidor: `btree_gist` 1.7 **disponible**, `trusted = true`,
`installed = false`. Al ser *trusted*, el dueño de la base con privilegio
`CREATE` podría instalarla sin superusuario, pero conviene validarlo con el DBA
y con el proveedor del entorno. No se instaló.

### 2.2 Alternativa B — trigger sin extensión

```sql
CREATE OR REPLACE FUNCTION fn_historial_tasa_sin_solape()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(NEW.contrato_id::text, 0));

    IF EXISTS (
        SELECT 1
        FROM historial_tasa h
        WHERE h.contrato_id = NEW.contrato_id
          AND h.id IS DISTINCT FROM NEW.id
          AND h.vigencia && NEW.vigencia
    ) THEN
        RAISE EXCEPTION 'Solape de vigencia para el contrato %', NEW.contrato_id
            USING ERRCODE = '23P01';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_historial_tasa_sin_solape
BEFORE INSERT OR UPDATE OF contrato_id, vigencia ON historial_tasa
FOR EACH ROW
EXECUTE FUNCTION fn_historial_tasa_sin_solape();
```

Detalles importantes:

- **Concurrencia:** dos transacciones podrían pasar el `EXISTS` a la vez. El
  `pg_advisory_xact_lock` derivado del `contrato_id` serializa las escrituras de
  un mismo contrato y se libera al terminar la transacción. Colisiones de hash
  solo serializan contratos distintos de forma ocasional, sin afectar
  corrección.
- Cubre `INSERT` y `UPDATE` de `contrato_id`/`vigencia`; el `DELETE` no genera
  solapes.
- Devuelve el mismo SQLSTATE que una restricción de exclusión (`23P01`), para
  que la aplicación trate igual ambos caminos.
- **No** protege frente a un superusuario que deshabilite el trigger
  (`ALTER TABLE ... DISABLE TRIGGER`), igual que una restricción puede ser
  eliminada por un superusuario.
- Coste: una consulta adicional por escritura; conviene el índice
  `(contrato_id, lower(vigencia))` que también crea N3.

| Ventaja | Inconveniente |
|---------|---------------|
| No requiere extensiones ni superusuario | Lógica y mantenimiento en PL/pgSQL |
| Portable a entornos gestionados restrictivos | Serializa escrituras por contrato |
| Mismo error para la aplicación | Más difícil de auditar que una restricción declarativa |

### 2.3 Decisión

- Si se aprueba `btree_gist`: usar **N3-A** y dejar N3-B comentado.
- Si no: comentar N3-A completo (incluido `CREATE EXTENSION`) y descomentar
  N3-B.
- No aplicar ambas: sería redundante y duplicaría el coste de escritura.

---

## 3. Semántica de la vigencia y fecha final (punto 2)

### 3.1 Definición

| Columna | Semántica |
|---------|-----------|
| `vigencia` | `daterange` **[inicio, fin)**: el inicio pertenece al tramo; el fin **no**. |
| `tasa` | Tasa efectiva anual aplicable, en fracción (`0.0950` = 9.5 %). |
| `fecha_registro` | Momento de captura (auditoría); no es la fecha de vigencia. |
| `motivo` | `INICIAL`, `REVISION`, `RENOVACION`, `AJUSTE`. |

`upper(vigencia)` infinito significa “sigue vigente sin fecha de término”.

### 3.2 Tratamiento de la fecha final

| Regla de negocio | Cómo se guarda |
|------------------|----------------|
| “Vigente desde el 1 de enero de 2026” | `[2026-01-01, infinity)` |
| “Vigente del 1 de enero al 30 de junio de 2026, **ambos inclusive**” | `[2026-01-01, 2026-07-01)` → se suma 1 día al fin |
| “Vigente del 1 de enero al 30 de junio de 2026, fin **exclusivo**” | `[2026-01-01, 2026-06-30)` |
| “Cambio de tasa el 1 de julio de 2026” | El tramo anterior termina en `2026-07-01` |

La conversión de una fecha final inclusiva a exclusiva es responsabilidad de la
capa que captura el dato (o del backfill). **Nunca** se guarda un rango con
extremo superior inclusivo.

### 3.3 Por qué [inicio, fin)

Con `[)` dos tramos consecutivos comparten el día de corte sin solaparse:

```
[2026-01-01, 2026-07-01)  y  [2026-07-01, infinity)
```

El 1 de julio pertenece solo al segundo tramo. Con `[]`, pertenecería a ambos y
la restricción de exclusión los rechazaría. Además, PostgreSQL canonicaliza los
rangos: `daterange(d1, d2, '[]')` se convierte en `[d1, d2+1)`, lo que introduce
ambigüedad si no se fija la convención.

### 3.4 Validaciones

```sql
CHECK (
    NOT isempty(vigencia)
    AND NOT lower_inf(vigencia)
    AND lower_inc(vigencia)
    AND NOT upper_inc(vigencia)
)
```

- `NOT isempty`: rechaza `[d, d)`.
- `NOT lower_inf`: exige fecha de inicio real.
- `lower_inc` / `NOT upper_inc`: obliga a la forma canónica `[)`.
- La no-superposición la aporta N3-A o N3-B.

### 3.5 Zona horaria

`CURRENT_DATE` se evalúa en la zona del servidor (el contenedor usa `UTC`). Si
“vigente hoy” debe calcularse en hora local de México, la aplicación debe pasar
la fecha explícita o la vista debe usar una fecha parametrizada. Se documenta
como supuesto; no se cambia la configuración del servidor.

### 3.6 Migración

Hoy `historial_tasa` tiene **0 filas**: no hay datos que reinterpretar. Si en el
futuro se migraran filas:

- si `fecha_fin` era el último día **inclusive** → `fecha_fin + 1`;
- si era un límite **exclusivo** → se usa tal cual;
- si era `NULL` → `infinity`.

No se debe adivinar: hay que validar con datos de muestra y con negocio.

---

## 4. Valores obligatorios de `valoracion_historica` (punto 3)

Se mantiene que `valor_mercado` y `valor_costo` sean **NOT NULL**, y se añade
`metodo` y `estado` obligatorios. Evaluación por clase de activo del alcance
inicial:

| Activo | `valor_mercado` | `valor_costo` | Método | `cotizacion_id` | Observaciones |
|--------|-----------------|---------------|--------|-----------------|---------------|
| Acciones | Cantidad × precio de cierre (o último disponible) | Costo de adquisición + comisiones | `COTIZACION_MERCADO` | Obligatorio | Si no hay cotización del día, usar `ULTIMA_COTIZACION` con estado `ESTIMADA`. |
| ETF | Igual que acciones | Igual que acciones | `COTIZACION_MERCADO` | Obligatorio | Mismo tratamiento; el precio puede provenir de otro mercado. |
| Criptomonedas | Cantidad × precio en el corte definido | Costo de adquisición + comisiones | `COTIZACION_MERCADO` / `ULTIMA_COTIZACION` | Obligatorio | Mercado 24/7: definir corte (p. ej. 00:00 UTC) y usar la última cotización en fines de semana. |
| SOFIPO / depósito a plazo | Capital + interés devengado (valor económico) | Principal aportado | `VALOR_DEVENGADO` | No aplica | No hay cotización de mercado; `fuente` puede ser la institución o “calculo”. Si hay penalización por retiro anticipado, reflejarla con `MODELO`. |
| PPR | Valor informado por el proveedor (o valor de las participaciones) | Aportaciones netas (aportes − retiros) | `PROVEEDOR` o `COTIZACION_MERCADO` | Obligatorio solo si es de mercado | Con `PROVEEDOR`, `fuente` es obligatoria (CHECK). |

**Conclusión:** el conjunto es adecuado para el alcance inicial, con tres
condiciones:

1. **Semántica definida:** `valor_mercado` es el valor económico (de mercado o
   devengado según el método) y `valor_costo` es el costo/aportaciones netas;
   nunca se usa `0` como sustituto de “sin dato”.
2. **Costo conocido:** toda posición debe tener costo de adquisición. Si se
   importa una posición sin historial, el costo se captura antes de valorar; no
   se relaja a NULL porque produciría una ganancia/pérdida falsa.
3. **Misma moneda:** ambos importes se guardan en `moneda_id`; la conversión se
   hace antes de insertar.

Cambios adicionales en v2:

- Métodos ampliados: `VALOR_DEVENGADO` (plazo/SOFIPO) y `PROVEEDOR` (PPR).
- `fuente` obligatoria cuando el método es `PROVEEDOR`.
- `ganancia_perdida` generada (`valor_mercado - valor_costo`) evita
  desincronización.
- Sin cotización disponible: no se inserta fila, o se arrastra la última con
  método/estado que lo declaren. Nunca se inventan precios ni series.

`valor_costo = 0` es válido (p. ej. posición totalmente retirada); `NULL` no.

---

## 5. Movimientos multidivisa (punto 4)

### 5.1 Diseño adoptado

| Columna | Contenido |
|---------|-----------|
| `moneda_id` | Moneda **original** de la operación (ya existe). |
| `importe` | Importe original con signo de flujo del portafolio (ya existe). |
| `importe_convertido` | El mismo flujo expresado en la **moneda de la posición**. |
| `tipo_cambio_aplicado` | Unidades de moneda de la posición por 1 unidad de `moneda_id`. |
| `fecha_tipo_cambio` | Fecha del tipo de cambio usado (puede diferir de la operación). |
| `fuente_tipo_cambio` | Origen del tipo de cambio (banco, proveedor, manual). |

### 5.2 Invariantes

1. Si `moneda_id` es la moneda de la posición: los tres campos de conversión son
   `NULL`.
2. Si son monedas distintas: los tres son `NOT NULL` (CHECK
   `ck_movimiento_conversion`).
3. `importe` e `importe_convertido` tienen el **mismo signo**.
4. `importe_convertido = importe × tipo_cambio_aplicado`, salvo redondeo a 8
   decimales.
5. La comisión permanece en la moneda del movimiento y **no** se incluye en
   `importe`; si se necesita en moneda de la posición, se deriva con la misma
   tasa.

Ejemplo conceptual (cifras arbitrarias): compra por `-100 USD`, tasa `17.50`
MXN/USD → `importe_convertido = -1750 MXN`. Se guardan ambos porque el original
es el hecho contractual y el convertido es el efecto contable en la posición.

### 5.3 Por qué no basta con una sola cifra

| Diseño | Problema |
|--------|----------|
| Guardar solo el importe convertido + tasa | Se pierde el importe original; al dividir reaparecen errores de redondeo. |
| Guardar solo el importe original + tasa | Los reportes en moneda de la posición recalculan con una tasa que puede haber cambiado o corregirse. |
| Guardar ambos + tasa (elegido) | Reproduce el asiento original y el efecto en la posición, y permite auditar la conversión. |

No se añade FK a `tipo_cambio` porque la tasa aplicada puede provenir del
contrato o del banco y no existir en el catálogo. Queda como decisión pendiente
un `tipo_cambio_id` opcional para cuando sí exista.

### 5.4 Validación de coherencia

Los CHECK garantizan presencia/signo de columnas, no la aritmética exacta. Una
validación opcional (trigger) podría comprobar
`abs(importe_convertido - importe * tipo_cambio_aplicado) <= tolerancia`.
No se incluye porque el redondeo varía por institución; se documenta como
pendiente y, si se implementa, con tolerancia configurable.

---

## 6. Renovaciones entre posiciones (punto 5)

Problema: `contrato_origen_id` por sí solo permitiría que un contrato de la
posición P2 se declare renovación de un contrato de P1.

Solución: clave compuesta + FK compuesta.

```sql
ALTER TABLE contrato_inversion
    ADD CONSTRAINT uq_contrato_id_posicion UNIQUE (id, posicion_id),
    ADD CONSTRAINT fk_contrato_origen
        FOREIGN KEY (contrato_origen_id, posicion_id)
        REFERENCES contrato_inversion (id, posicion_id)
        ON DELETE RESTRICT;
```

Qué garantiza:

- El contrato origen pertenece a la **misma** `posicion_id` que el renovado.
- Si `contrato_origen_id` es `NULL`, la FK no se evalúa (`MATCH SIMPLE`), lo que
  permite contratos iniciales.
- `ON DELETE RESTRICT` impide borrar el origen mientras exista la renovación.
- Cambiar `posicion_id` de un contrato referenciado queda bloqueado por la FK.

Qué **no** garantiza (pendiente):

- Que no haya ciclos (A→B→A). Requeriría trigger con CTE recursiva.
- Que la fecha de inicio de la renovación sea posterior al origen.
- Que exista una sola renovación por contrato (podría haber varias ramas).

Con 0 filas y un modelo nuevo, se propone dejar esos controles a la aplicación
salvo que se apruebe un trigger adicional.

La relación sigue siendo **una sola**: `contrato_inversion.posicion_id` (N
contratos → 1 posición). Se elimina `posicion.contrato_id` (N1).

---

## 7. Reinversión e interés compuesto (punto 6)

El CHECK de v1:

```sql
CHECK (NOT reinvierte_intereses OR modalidad_interes = 'COMPUESTO')  -- v1
```

**era demasiado restrictivo** porque mezcla dos conceptos:

| Concepto | Columna | Qué describe |
|----------|---------|--------------|
| Devengo contractual | `modalidad_interes` | Cómo se calcula el interés (simple/compuesto). |
| Destino del rendimiento | `reinvierte_intereses` | Qué se hace con el efectivo pagado: reinvertir o retirar. |

Casos válidos que v1 rechazaba:

- Interés **simple** pagado mensualmente y reinvertido por el inversionista en el
  mismo instrumento.
- Interés **simple** al vencimiento, cuyo capital + intereses se renueva en un
  contrato nuevo.
- Contrato **compuesto** que, por acuerdo, paga una parte de los intereses.

En v2 **no se agrega** ese CHECK. `reinvierte_intereses` significa “los
rendimientos en efectivo se destinan a reinvertir”; no implica capitalización.
La capitalización se deriva de `modalidad_interes = 'COMPUESTO'` junto con la
ausencia de pago periódico, y se documenta en la aplicación.

Si en el futuro se quiere una regla específica por producto, debe ser una
validación de negocio (por `tipo_instrumento`), no una restricción global.

---

## 8. Relación posición–contrato (punto 8)

- Se conserva `contrato_inversion.posicion_id` NOT NULL.
- Se permite **N contratos por posición** (renovaciones, tramos).
- Se elimina `posicion.contrato_id`, su FK `fk_posicion_contrato` y el índice
  `idx_posicion_contrato`.
- Los históricos no cambian: `historial_tasa` y `pago_rendimiento` referencian
  el contrato; `valoracion_historica` y `movimiento`, la posición.

---

## 9. Cambios necesarios vs. opcionales (punto 7)

### 9.1 Necesarios (`N1–N5`, activos en el SQL)

| ID | Cambio | Propósito | Impacto |
|----|--------|-----------|---------|
| N1 | Quitar `posicion.contrato_id` | Eliminar relación duplicada (decisión confirmada) | Se pierde una vía sin uso; históricos intactos |
| N2 | Campos de contrato + FK compuesta de renovación | Modelar fija/variable, base de días, renovaciones coherentes | Más columnas y validaciones; índice extra |
| N3 | `historial_tasa.vigencia` + no solape | Semántica única y protección de datos | Requiere extensión o trigger |
| N4 | Valoraciones obligatorias + método/estado + ganancia generada | Consistencia y trazabilidad por clase de activo | Inserts más estrictos |
| N5 | Importes original/convertido + tasa/fecha/fuente | Operaciones multidivisa auditables | Tres columnas y CHECK |

### 9.2 Opcionales (bloque comentado)

| ID | Cambio | Cuándo conviene |
|----|--------|-----------------|
| O1 | `posicion.lote` + índice `(cuenta_id, instrumento_id)` | Si se quieren distinguir lotes del mismo instrumento |
| O2 | `movimiento.movimiento_origen_id` + índices | Si se necesita trazabilidad de reversas/correcciones |
| O3 | `tipo_movimiento.naturaleza` | Si los reportes la usan en lugar de deducir signos |
| O4 | Tabla `plan_ppr` | Solo si se gestionan reglas propias del plan (aportes, límite deducible, retiro) |
| O5 | Vista `v_contrato_tasa_vigente` | Comodidad de consulta; no es necesaria para la integridad |

`plan_ppr` y la vista **no** forman parte del bloque obligatorio. El PPR se puede
representar sin `plan_ppr` como `cuenta_financiera` (tipo PPR) con posiciones en
sus instrumentos; la tabla solo añade metadatos del plan.

### 9.3 Pendientes de decisión

- Aprobar `btree_gist` o usar el trigger.
- Impedir ciclos de renovación y validar orden de fechas (trigger opcional).
- Tolerancia para validar `importe_convertido` (trigger opcional).
- FK opcional a `tipo_cambio` cuando la tasa provenga del catálogo.
- Varias valoraciones por día (hoy se asume una).
- `fuente` obligatoria en `cotizacion` (hoy opcional).
- Método de costo (FIFO/promedio) para `precio_promedio`.
- Estados de contrato (derivados vs columna).
- Carga de catálogos.

---

## 10. Verificación posterior a la aprobación

Nada de esto se ejecutó. Debe correr en una base de ensayo primero y con
`pg_dump` previo.

### 10.1 Antes de aplicar

1. Confirmar estado: 20 tablas, 0 filas, `btree_gist` no instalada.
2. Decidir N3-A o N3-B y editar el SQL en consecuencia.
3. Probar el script completo en una copia.
4. Backup: `pg_dump -Fc`.

### 10.2 Verificación estructural

```sql
-- N1: la columna ya no existe
SELECT count(*) FROM information_schema.columns
 WHERE table_schema = 'public' AND table_name = 'posicion'
   AND column_name = 'contrato_id';           -- 0

-- N2: FK compuesta de renovación
SELECT pg_get_constraintdef(oid)
  FROM pg_constraint
 WHERE conname = 'fk_contrato_origen';        -- (contrato_origen_id, posicion_id) → (id, posicion_id)

-- N3: exclusión (si se usó N3-A)
SELECT conname FROM pg_constraint
 WHERE conrelid = 'historial_tasa'::regclass AND contype = 'x';
-- Si se usó N3-B: SELECT tgname FROM pg_trigger WHERE tgrelid = 'historial_tasa'::regclass;

-- N3: columnas antiguas eliminadas
SELECT count(*) FROM information_schema.columns
 WHERE table_name = 'historial_tasa' AND column_name IN ('fecha','fecha_inicio','fecha_fin');  -- 0

-- N4: obligatorios y columna generada
SELECT attname, attnotnull, attgenerated
  FROM pg_attribute
 WHERE attrelid = 'valoracion_historica'::regclass
   AND attname IN ('valor_mercado','valor_costo','ganancia_perdida');

-- N5: columnas de conversión
SELECT column_name FROM information_schema.columns
 WHERE table_name = 'movimiento'
   AND column_name IN ('importe_convertido','tipo_cambio_aplicado','fecha_tipo_cambio','fuente_tipo_cambio');
```

### 10.3 Pruebas de comportamiento (transacción + ROLLBACK)

| Prueba | Resultado esperado | SQLSTATE |
|--------|--------------------|----------|
| Dos tramos contiguos `[d1,d2)` y `[d2,d3)` | Se aceptan | — |
| Dos tramos solapados | Falla | `23P01` |
| Renovación con origen de otra posición | Falla | `23503` |
| Renovación con origen de la misma posición | Se acepta | — |
| Contrato FIJA sin `tasa_contractual` | Falla | `23514` |
| Contrato VARIABLE sin `tasa_referencia` | Falla | `23514` |
| Reinvertir con interés SIMPLE | Se acepta (sin CHECK) | — |
| `valor_mercado` NULL | Falla | `23502` |
| Método `COTIZACION_MERCADO` sin `cotizacion_id` | Falla | `23514` |
| Método `PROVEEDOR` sin `fuente` | Falla | `23514` |
| `ganancia_perdida` tras insertar 1100/1000 | Devuelve 100 | — |
| Movimiento con tasa pero sin importe convertido | Falla | `23514` |
| Movimiento con importe convertido y tasa válidos | Se acepta | — |

Las pruebas de inserción requieren crear padres mínimos (moneda, portafolio,
institución, tipo de cuenta, cuenta, instrumento, posición) dentro de la misma
transacción y hacer `ROLLBACK` al final. No se ejecutaron.

### 10.4 Criterios de aceptación

- 0 claves foráneas sin validar.
- La relación posición–contrato existe una sola vez.
- Los históricos siguen apuntando a `contrato_inversion(id)`.
- No hay diferencias inesperadas en `pg_dump` respecto al cambio aprobado.
- La base permanece con 0 filas de datos reales.

---

## 11. Supuestos

1. Tasas en fracción anual (`0.0950` = 9.5 %), no porcentaje.
2. Fechas de negocio sin hora; `CURRENT_DATE` del servidor (UTC).
3. Una valoración por posición y día.
4. `valor_costo` incluye comisiones atribuibles.
5. `valor_mercado` y `valor_costo` en la misma moneda.
6. `importe` es flujo de caja del portafolio y excluye comisión.
7. La moneda de la posición no cambia después de crearla.
8. La tasa aplicada se guarda como instantánea (no se recalcula).
9. `plan_ppr` y la vista son opcionales.
10. Los catálogos se cargan en otra fase.

---

## 12. Archivos y confirmación

| Archivo | Estado |
|---------|--------|
| `docs/schema-change-proposal-v2.md` | Este documento (nuevo) |
| `docs/schema-change-proposal-v2.sql` | SQL v2 (nuevo, **no ejecutado**) |
| `docs/schema-change-proposal.md` / `.sql` | v1, conservados para comparación |

Confirmación: no se ejecutó ningún `CREATE`, `ALTER`, `DROP`, `TRUNCATE`,
`INSERT`, `UPDATE` ni `DELETE`; no se instalaron extensiones; no se modificaron
el contenedor, el volumen ni la configuración. Solo se hicieron consultas de
lectura al catálogo.
