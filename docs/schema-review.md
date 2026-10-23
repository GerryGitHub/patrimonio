# Revisión del esquema — Patrimonio

Revisión de solo lectura del esquema aplicado en PostgreSQL 16.11.

| Dato | Valor |
|------|-------|
| Base de datos | `patrimonio` (esquema `public`) |
| Contenedor | `patrimonio-postgres` (postgres:16) |
| Fecha de revisión | 2026-10-23 |
| Método | `pg_dump --schema-only` + consultas al catálogo (`pg_catalog`, `information_schema`) |
| Operaciones de escritura | **Ninguna** (sin `DROP`, `ALTER`, `TRUNCATE`, `INSERT`, `UPDATE`, `DELETE`) |
| Tablas | 20 |
| Restricciones | 78 (20 PK, 28 FK, 12 UNIQUE, 18 CHECK) |
| Índices | 62 (30 explícitos + 32 de PK/UNIQUE) |
| Filas totales | 0 |
| Tamaño de la base | 8.5 MB |

## 1. Archivos generados

| Archivo | Contenido |
|---------|-----------|
| `docs/schema-review.sql` | DDL completo real: 20 `CREATE TABLE`, 20 PK, 28 FK, 12 UNIQUE, 18 CHECK, 30 `CREATE INDEX` explícitos, comentarios. 724 líneas, 31,094 bytes |
| `docs/er-diagram.md` | Diagrama Mermaid con las 28 FK reales, cardinalidades y análisis de la doble relación posición/contrato |
| `docs/schema-review.md` | Este documento |

Huellas del dump (para detectar cambios futuros):

- MD5 del archivo tal cual: `53E5C514004ED7A4E4EA2D5E3EC01A91`
- MD5 normalizado: `BE137784728938894F941E88D9524EBE`
  (se excluye la línea `\restrict <token>` que `pg_dump` 16.11 genera con un token
  aleatorio en cada ejecución; no forma parte del esquema)

## 2. Método de obtención (solo lectura)

```bash
# DDL completo, sin propietarios ni privilegios (no incluye datos)
docker compose exec -T postgres pg_dump \
  --schema-only --no-owner --no-privileges \
  -U patrimonio -d patrimonio > docs/schema-review.sql

# Inventario y verificación desde el catálogo
docker compose exec -T postgres psql -U patrimonio -d patrimonio -c "\dt"
docker compose exec -T postgres psql -U patrimonio -d patrimonio -c "SELECT ... FROM pg_constraint ..."
docker compose exec -T postgres psql -U patrimonio -d patrimonio -c "SELECT ... FROM pg_indexes ..."
docker compose exec -T postgres psql -U patrimonio -d patrimonio -c "SELECT count(*) FROM <tabla>"
```

En PowerShell el dump se escribió con `UTF8Encoding(false)` para evitar BOM.

## 3. Inventario de tablas

| # | Tabla | Grupo |
|---|-------|-------|
| 1 | `usuario` | Usuarios y portafolios |
| 2 | `portafolio` | Usuarios y portafolios |
| 3 | `membresia` | Usuarios y portafolios |
| 4 | `institucion` | Instituciones y cuentas |
| 5 | `cuenta_financiera` | Instituciones y cuentas |
| 6 | `moneda` | Catálogos |
| 7 | `clase_activo` | Catálogos |
| 8 | `tipo_instrumento` | Catálogos |
| 9 | `tipo_cuenta` | Catálogos |
| 10 | `tipo_movimiento` | Catálogos |
| 11 | `tipo_cambio` | Mercado |
| 12 | `instrumento` | Instrumentos |
| 13 | `identificador_instrumento` | Instrumentos |
| 14 | `posicion` | Posiciones |
| 15 | `movimiento` | Posiciones |
| 16 | `contrato_inversion` | Contratos |
| 17 | `historial_tasa` | Contratos |
| 18 | `pago_rendimiento` | Contratos |
| 19 | `cotizacion` | Mercado |
| 20 | `valoracion_historica` | Mercado |

El DDL completo de las 20 tablas, con tipos, defaults, PK, FK, UNIQUE, CHECK e
índices, está en `docs/schema-review.sql`.

## 4. Relación `posicion` ↔ `contrato_inversion`

### 4.1 Definición real de `posicion`

```sql
CREATE TABLE public.posicion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    cuenta_id uuid NOT NULL,
    instrumento_id uuid NOT NULL,
    contrato_id uuid,
    cantidad numeric(28,10) DEFAULT 0 NOT NULL,
    precio_promedio numeric(24,8) DEFAULT 0 NOT NULL,
    moneda_id uuid NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    fecha_apertura timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_posicion_fechas CHECK ((fecha_actualizacion >= fecha_apertura)),
    CONSTRAINT ck_posicion_precio_promedio CHECK ((precio_promedio >= (0)::numeric))
);
```

| Restricción | Tipo | Definición |
|-------------|------|------------|
| `posicion_pkey` | PK | `(id)` |
| `posicion_cuenta_id_fkey` | FK | `cuenta_id → cuenta_financiera(id) ON DELETE RESTRICT` |
| `posicion_instrumento_id_fkey` | FK | `instrumento_id → instrumento(id) ON DELETE RESTRICT` |
| `posicion_moneda_id_fkey` | FK | `moneda_id → moneda(id) ON DELETE RESTRICT` |
| `fk_posicion_contrato` | FK | `contrato_id → contrato_inversion(id) ON DELETE RESTRICT` |
| `ck_posicion_precio_promedio` | CHECK | `precio_promedio >= 0` |
| `ck_posicion_fechas` | CHECK | `fecha_actualizacion >= fecha_apertura` |

Índices: `posicion_pkey (id)`, `idx_posicion_cuenta (cuenta_id)`,
`idx_posicion_instrumento (instrumento_id)`, `idx_posicion_moneda (moneda_id)`,
`idx_posicion_contrato (contrato_id)`.

### 4.2 Definición real de `contrato_inversion`

```sql
CREATE TABLE public.contrato_inversion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    posicion_id uuid NOT NULL,
    tasa numeric(12,8) NOT NULL,
    modalidad_interes character varying(30) NOT NULL,
    periodicidad_calculo character varying(30),
    periodicidad_pago character varying(30),
    fecha_inicio date NOT NULL,
    fecha_vencimiento date,
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_contrato_fechas CHECK (((fecha_vencimiento IS NULL) OR (fecha_vencimiento >= fecha_inicio))),
    CONSTRAINT ck_contrato_modalidad CHECK (((modalidad_interes)::text = ANY ((ARRAY['SIMPLE'::character varying, 'COMPUESTO'::character varying])::text[]))),
    CONSTRAINT ck_contrato_tasa CHECK ((tasa >= (0)::numeric))
);
```

| Restricción | Tipo | Definición |
|-------------|------|------------|
| `contrato_inversion_pkey` | PK | `(id)` |
| `contrato_inversion_posicion_id_fkey` | FK | `posicion_id → posicion(id) ON DELETE RESTRICT` |
| `ck_contrato_tasa` | CHECK | `tasa >= 0` |
| `ck_contrato_modalidad` | CHECK | `modalidad_interes IN ('SIMPLE','COMPUESTO')` |
| `ck_contrato_fechas` | CHECK | `fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_inicio` |

Índices: `contrato_inversion_pkey (id)`, `idx_contrato_inversion_posicion (posicion_id)`.

### 4.3 ¿`contrato_id` duplica la relación?

**Sí, la relación está representada dos veces y de forma asimétrica.**

| Vía | Columna | Nulabilidad | Cardinalidad permitida | Índice | UNIQUE |
|-----|---------|-------------|------------------------|--------|--------|
| A | `contrato_inversion.posicion_id` | NOT NULL | N contratos → 1 posición | sí | no |
| B | `posicion.contrato_id` | NULL | N posiciones → 1 contrato | sí | no |

Consecuencias del estado actual:

- No existe ninguna restricción que garantice coherencia entre ambas columnas:
  la posición A puede apuntar al contrato C aunque C apunte a la posición B.
- Se permiten cadenas incoherentes: `posicion(contrato_id=C)` y
  `contrato_inversion(C, posicion_id=P2)` con `P2 <> A`.
- `posicion.contrato_id` es opcional, por lo que una posición puede no tener
  contrato asociado; el ER no aclara si eso es válido.
- El borrado sigue siendo seguro (todo `RESTRICT`), pero las dos FK complican
  cualquier operación de mantenimiento posterior.

Opciones (ninguna aplicada, requieren autorización):

1. **Recomendada — una sola vía:** conservar `contrato_inversion.posicion_id`
   (NOT NULL, ya usada por `historial_tasa` y `pago_rendimiento`) y eliminar
   `posicion.contrato_id`, su FK `fk_posicion_contrato` y el índice
   `idx_posicion_contrato`. Un contrato pertenece a una posición; la posición no
   necesita conocer "su" contrato.
2. **Si un contrato agrupa varias posiciones:** mantener solo
   `posicion.contrato_id` y quitar `contrato_inversion.posicion_id` (cambio mayor:
   afecta la semántica de los históricos).
3. **Si la relación es realmente 1:1:** dejar una única columna y añadir
   `UNIQUE`, eliminando la otra vía.
4. **Mantener ambas** exigiría un `UNIQUE` en `posicion.contrato_id` y un trigger
   de consistencia bidireccional; se desaconseja por complejidad.

No se ejecutó ningún `ALTER TABLE`: la base conserva la estructura descrita.

## 5. Tablas históricas

### 5.1 `historial_tasa`

```sql
CREATE TABLE public.historial_tasa (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    contrato_id uuid NOT NULL,
    tasa numeric(12,8) NOT NULL,
    fecha date NOT NULL,
    fecha_inicio date,
    fecha_fin date,
    CONSTRAINT ck_historial_tasa_fechas CHECK (((fecha_fin IS NULL) OR (fecha_inicio IS NULL) OR (fecha_fin >= fecha_inicio))),
    CONSTRAINT ck_historial_tasa_tasa CHECK ((tasa >= (0)::numeric))
);
```

- PK `(id)`; FK `contrato_id → contrato_inversion(id) ON DELETE RESTRICT`.
- UNIQUE `(contrato_id, fecha)` → una tasa por contrato y fecha.
- Índices: `idx_historial_tasa_contrato_fecha (contrato_id, fecha DESC)`.
- `fecha` = fecha del registro; `fecha_inicio`/`fecha_fin` = vigencia del tramo.

### 5.2 `valoracion_historica`

```sql
CREATE TABLE public.valoracion_historica (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    posicion_id uuid NOT NULL,
    fecha date NOT NULL,
    valor_mercado numeric(24,8),
    valor_costo numeric(24,8),
    ganancia_perdida numeric(24,8),
    moneda_id uuid NOT NULL
);
```

- PK `(id)`; FK `posicion_id → posicion(id)`, `moneda_id → moneda(id)`, ambas RESTRICT.
- UNIQUE `(posicion_id, fecha)` → una valoración por posición y fecha.
- Índices: `idx_valoracion_historica_posicion_fecha (posicion_id, fecha DESC)`,
  `idx_valoracion_historica_moneda (moneda_id)`.
- **Observación:** `valor_mercado`, `valor_costo` y `ganancia_perdida` admiten NULL;
  se puede guardar una valoración sin ningún importe. No hay CHECK de coherencia
  (`ganancia_perdida = valor_mercado - valor_costo`).

### 5.3 `cotizacion`

```sql
CREATE TABLE public.cotizacion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    instrumento_id uuid NOT NULL,
    fecha date NOT NULL,
    precio numeric(24,8) NOT NULL,
    moneda_id uuid NOT NULL,
    fuente character varying(100),
    CONSTRAINT ck_cotizacion_precio CHECK ((precio >= (0)::numeric))
);
```

- PK `(id)`; FK `instrumento_id → instrumento(id)`, `moneda_id → moneda(id)`, RESTRICT.
- UNIQUE `(instrumento_id, fecha, moneda_id)` → una cotización por instrumento,
  fecha y moneda.
- Índices: `idx_cotizacion_instrumento_fecha (instrumento_id, fecha DESC)`,
  `idx_cotizacion_moneda (moneda_id)`.
- `precio` obligatorio y no negativo; `fuente` libre (sin catálogo).

### 5.4 `movimiento`

```sql
CREATE TABLE public.movimiento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    posicion_id uuid NOT NULL,
    tipo_movimiento_id uuid NOT NULL,
    fecha_operacion date NOT NULL,
    cantidad numeric(28,10) NOT NULL,
    precio numeric(24,8) NOT NULL,
    importe numeric(24,8) NOT NULL,
    moneda_id uuid NOT NULL,
    comision numeric(20,8) DEFAULT 0 NOT NULL,
    descripcion character varying(500),
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_movimiento_comision CHECK ((comision >= (0)::numeric)),
    CONSTRAINT ck_movimiento_precio CHECK ((precio >= (0)::numeric))
);
```

- PK `(id)`; FK `posicion_id → posicion(id)`, `tipo_movimiento_id → tipo_movimiento(id)`,
  `moneda_id → moneda(id)`, todas RESTRICT.
- Índices: `idx_movimiento_posicion`, `idx_movimiento_tipo`, `idx_movimiento_moneda`,
  `idx_movimiento_fecha (fecha_operacion)`.
- Sin UNIQUE: varios movimientos por posición y fecha (correcto: compras/ventas).
- **Observaciones:** `cantidad` admite negativos (no hay CHECK ≥ 0), lo que permite
  ventas si se modela cantidad negativa; `importe` no se valida contra
  `cantidad * precio`; `moneda_id` del movimiento puede diferir de
  `posicion.moneda_id` sin restricción.

### 5.5 `pago_rendimiento`

```sql
CREATE TABLE public.pago_rendimiento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    contrato_id uuid NOT NULL,
    fecha_pago date NOT NULL,
    importe numeric(24,8) NOT NULL,
    moneda_id uuid NOT NULL,
    tipo character varying(50),
    descripcion character varying(500),
    CONSTRAINT ck_pago_rendimiento_importe CHECK ((importe >= (0)::numeric))
);
```

- PK `(id)`; FK `contrato_id → contrato_inversion(id)`, `moneda_id → moneda(id)`, RESTRICT.
- Índices: `idx_pago_rendimiento_contrato`, `idx_pago_rendimiento_moneda`.
- Sin UNIQUE natural: se pueden registrar pagos duplicados para el mismo contrato
  y fecha. `tipo` es texto libre (no catálogo) y `importe` no admite negativos.

## 6. Catálogos y datos

Todas las tablas están vacías, incluidos los catálogos:

| Tabla | Filas | Tabla | Filas |
|-------|------:|-------|------:|
| `usuario` | 0 | `clase_activo` | 0 |
| `portafolio` | 0 | `tipo_instrumento` | 0 |
| `membresia` | 0 | `instrumento` | 0 |
| `institucion` | 0 | `identificador_instrumento` | 0 |
| `cuenta_financiera` | 0 | `posicion` | 0 |
| `moneda` | 0 | `movimiento` | 0 |
| `tipo_cuenta` | 0 | `contrato_inversion` | 0 |
| `tipo_movimiento` | 0 | `historial_tasa` | 0 |
| `tipo_cambio` | 0 | `pago_rendimiento` | 0 |
| `cotizacion` | 0 | `valoracion_historica` | 0 |

- **Catálogos (`moneda`, `clase_activo`, `tipo_instrumento`, `tipo_cuenta`,
  `tipo_movimiento`): sin registros.** No se cargó ningún dato semilla.
- No hay datos personales, financieros ni credenciales en la base.
- El `pg_dump` se generó con `--schema-only`: el archivo no contiene datos.

## 7. Verificación de que la base no se modificó

1. Todas las sentencias ejecutadas fueron `pg_dump` y `SELECT` (incluye `\dt`
   y consultas a `pg_catalog`/`information_schema`).
2. No se ejecutaron `DROP`, `ALTER`, `TRUNCATE`, `INSERT`, `UPDATE`, `DELETE`,
   `CREATE` ni `GRANT`.
3. Se repitió el `pg_dump` al finalizar: el contenido es idéntico al guardado,
   salvo la línea `\restrict <token>` que `pg_dump` genera aleatoriamente.
   Comparación normalizada (sin esa línea): **idéntica**.
4. Conteos antes y después de la revisión:

   | Métrica | Antes | Después |
   |---------|-------|---------|
   | Tablas en `public` | 20 | 20 |
   | Restricciones | 78 | 78 |
   | Índices | 62 | 62 |
   | Filas totales | 0 | 0 |

5. Contenedor: `Up (healthy)`, `FailingStreak=0`. No se recreó, no se tocó el
   volumen `patrimonio_pgdata` ni la configuración de Compose.

## 8. Hallazgos y recomendaciones (sin cambios aplicados)

| # | Hallazgo | Impacto | Recomendación |
|---|----------|---------|---------------|
| 1 | Doble relación `posicion.contrato_id` ↔ `contrato_inversion.posicion_id` | Datos incoherentes posibles | Eliminar `posicion.contrato_id` (opción 1 del §4.3) |
| 2 | Sin UNIQUE en `posicion(cuenta_id, instrumento_id)` | Se pueden duplicar posiciones del mismo instrumento en una cuenta | Definir si los lotes por contrato son válidos; si no, añadir UNIQUE |
| 3 | `valoracion_historica` admite métricas NULL | Valoraciones vacías | Añadir `NOT NULL` si toda valoración debe tener importes |
| 4 | `pago_rendimiento` sin clave natural | Pagos duplicados | Evaluar UNIQUE `(contrato_id, fecha_pago, tipo)` |
| 5 | `movimiento` no valida `importe = cantidad * precio` | Inconsistencias contables | Validar en aplicación o trigger |
| 6 | `fecha_actualizacion` sin trigger | Se queda obsoleta si la app no la actualiza | Trigger `BEFORE UPDATE` o gestionarla en la aplicación |
| 7 | Catálogos vacíos | No se puede operar sin monedas/tipos | Cargar catálogo en una fase posterior |
| 8 | `usuario.email` con CHECK de formato | Rechaza correos válidos poco comunes | Validación en aplicación si molesta |
| 9 | Todo `ON DELETE RESTRICT` | No hay borrado en cascada | Correcto si la baja es lógica (`activo = false`) |

Ninguna de estas recomendaciones se implementó: la revisión fue exclusivamente de
lectura.
