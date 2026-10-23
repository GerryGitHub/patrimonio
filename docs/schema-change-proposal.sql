-- =====================================================================
-- Patrimonio - PROPUESTA de cambios de esquema
-- =====================================================================
-- ADVERTENCIA: este archivo NO se ha ejecutado. Es material de revision.
--              La base sigue exactamente como la describe
--              docs/schema-review.sql (20 tablas, 0 filas).
--
-- Motor: PostgreSQL 16.11. Base: patrimonio. Esquema: public.
--
-- Orden de aplicacion sugerido (una sola transaccion):
--   1. Obligatorio: eliminar posicion.contrato_id (relacion duplicada)
--   2. Contratos de inversion
--   3. Historial de tasas (rango + exclusion)
--   4. Valoraciones historicas
--   5. Movimientos, tipos de movimiento y posiciones
--   6. Opcional: plan PPR y vista de tasa vigente
--
-- Prerrequisito de la seccion 3:
--   btree_gist esta DISPONIBLE (1.7) pero NO instalado. Requiere superusuario.
--   Sin el, la restriccion EXCLUDE no se puede crear; existe alternativa con
--   trigger documentada en docs/schema-change-proposal.md.
--
-- Antes de aplicar: pg_dump de seguridad y ventana de mantenimiento.
-- =====================================================================

BEGIN;

-- =====================================================================
-- 1. OBLIGATORIO - Relacion duplicada posicion <-> contrato
-- =====================================================================
-- Decision confirmada: una posicion puede tener N contratos (renovaciones).
-- La unica via necesaria es contrato_inversion.posicion_id.
-- Los historicos (historial_tasa, pago_rendimiento) cuelgan del contrato,
-- por lo que siguen apuntando al contrato correcto sin cambios.

ALTER TABLE posicion DROP CONSTRAINT fk_posicion_contrato;
DROP INDEX idx_posicion_contrato;
ALTER TABLE posicion DROP COLUMN contrato_id;

-- =====================================================================
-- 2. RECOMENDADO - Contratos de inversion
-- =====================================================================
-- tasa_contractual = tasa pactada (fija) o tasa inicial (variable).
-- La tasa vigente NO se almacena: se obtiene de historial_tasa.
-- Evita duplicar el mismo dato en dos sitios.

ALTER TABLE contrato_inversion RENAME COLUMN tasa TO tasa_contractual;
ALTER TABLE contrato_inversion RENAME CONSTRAINT ck_contrato_tasa TO ck_contrato_tasa_contractual;
ALTER TABLE contrato_inversion ALTER COLUMN tasa_contractual DROP NOT NULL;

ALTER TABLE contrato_inversion
    ADD COLUMN tipo_tasa varchar(10) NOT NULL DEFAULT 'FIJA',
    ADD COLUMN tasa_referencia varchar(50),
    ADD COLUMN spread numeric(12,8),
    ADD COLUMN base_calculo_dias varchar(10),
    ADD COLUMN reinvierte_intereses boolean NOT NULL DEFAULT false,
    ADD COLUMN renovacion_automatica boolean NOT NULL DEFAULT false,
    ADD COLUMN contrato_origen_id uuid;

ALTER TABLE contrato_inversion ALTER COLUMN tipo_tasa DROP DEFAULT;

ALTER TABLE contrato_inversion
    ADD CONSTRAINT ck_contrato_tipo_tasa
        CHECK (tipo_tasa IN ('FIJA', 'VARIABLE')),
    ADD CONSTRAINT ck_contrato_tasa_segun_tipo
        CHECK (
            (tipo_tasa = 'FIJA' AND tasa_contractual IS NOT NULL AND tasa_referencia IS NULL)
            OR (tipo_tasa = 'VARIABLE' AND tasa_referencia IS NOT NULL)
        ),
    ADD CONSTRAINT ck_contrato_base_dias
        CHECK (base_calculo_dias IS NULL OR base_calculo_dias IN ('ACT/360', 'ACT/365', '30/360')),
    ADD CONSTRAINT ck_contrato_periodicidad_calculo
        CHECK (periodicidad_calculo IS NULL OR periodicidad_calculo IN
               ('DIARIA', 'SEMANAL', 'MENSUAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL', 'AL_VENCIMIENTO')),
    ADD CONSTRAINT ck_contrato_periodicidad_pago
        CHECK (periodicidad_pago IS NULL OR periodicidad_pago IN
               ('DIARIA', 'SEMANAL', 'MENSUAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL', 'AL_VENCIMIENTO', 'UNICA')),
    ADD CONSTRAINT ck_contrato_reinversion
        CHECK (NOT reinvierte_intereses OR modalidad_interes = 'COMPUESTO'),
    ADD CONSTRAINT fk_contrato_origen
        FOREIGN KEY (contrato_origen_id) REFERENCES contrato_inversion (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_contrato_origen_distinto
        CHECK (contrato_origen_id IS NULL OR contrato_origen_id <> id);

CREATE INDEX idx_contrato_inversion_posicion_fecha
    ON contrato_inversion (posicion_id, fecha_inicio DESC);
DROP INDEX idx_contrato_inversion_posicion;
CREATE INDEX idx_contrato_inversion_origen
    ON contrato_inversion (contrato_origen_id);

COMMENT ON COLUMN contrato_inversion.tasa_contractual
    IS 'Tasa pactada (fija, decimal anual: 0.0950 = 9.5%) o tasa inicial (variable)';
COMMENT ON COLUMN contrato_inversion.tasa_referencia
    IS 'Indice de referencia para tasa variable (TIIE, SOFR, etc.)';
COMMENT ON COLUMN contrato_inversion.spread
    IS 'Margen sobre la referencia; la tasa efectiva se registra en historial_tasa';
COMMENT ON COLUMN contrato_inversion.base_calculo_dias
    IS 'Convencion de dias para el devengo: ACT/360, ACT/365, 30/360';
COMMENT ON COLUMN contrato_inversion.reinvierte_intereses
    IS 'true = los intereses se capitalizan (requiere modalidad COMPUESTO)';

-- =====================================================================
-- 3. RECOMENDADO - Historial de tasas como rango de vigencia
-- =====================================================================
-- Semantica propuesta:
--   vigencia     = [fecha_inicio, fecha_fin): inicio incluido, fin excluido.
--                  upper infinito = tasa vigente sin fecha de termino.
--   fecha_registro = cuando se capturo la tasa (auditoria).
--   tasa         = tasa efectiva anual aplicable, decimal (0.0950 = 9.5%).
-- La regla [) permite que dos tramos consecutivos compartan el dia de corte
-- sin considerarse traslapados.

-- Descomentar tras aprobar la extension (requiere superusuario):
-- CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE historial_tasa
    ADD COLUMN vigencia daterange,
    ADD COLUMN fecha_registro timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN motivo varchar(30);

-- Backfill (hoy la tabla tiene 0 filas; se deja explicito para el futuro):
UPDATE historial_tasa
SET vigencia = daterange(
        COALESCE(fecha_inicio, fecha),
        COALESCE(fecha_fin, 'infinity'::date),
        '[)'
    );

ALTER TABLE historial_tasa ALTER COLUMN vigencia SET NOT NULL;

ALTER TABLE historial_tasa DROP CONSTRAINT uq_historial_tasa;
ALTER TABLE historial_tasa DROP COLUMN fecha;
ALTER TABLE historial_tasa DROP COLUMN fecha_inicio;
ALTER TABLE historial_tasa DROP COLUMN fecha_fin;

ALTER TABLE historial_tasa
    ADD CONSTRAINT ck_historial_tasa_vigencia
        CHECK (NOT isempty(vigencia) AND lower_inc(vigencia) AND NOT upper_inc(vigencia)),
    ADD CONSTRAINT ck_historial_tasa_motivo
        CHECK (motivo IS NULL OR motivo IN ('INICIAL', 'REVISION', 'RENOVACION', 'AJUSTE')),
    ADD CONSTRAINT ex_historial_tasa_sin_solape
        EXCLUDE USING gist (contrato_id WITH =, vigencia WITH &&);

DROP INDEX idx_historial_tasa_contrato_fecha;
CREATE INDEX idx_historial_tasa_contrato_vigencia
    ON historial_tasa (contrato_id, lower(vigencia) DESC);

COMMENT ON COLUMN historial_tasa.vigencia
    IS 'Intervalo [inicio, fin) de vigencia de la tasa; upper infinito si sigue vigente';

-- =====================================================================
-- 4. RECOMENDADO - Valoraciones historicas
-- =====================================================================
-- La cotizacion (precio de mercado por instrumento) y la valoracion
-- (valor de una posicion) siguen siendo entidades distintas.

ALTER TABLE valoracion_historica
    ADD COLUMN metodo varchar(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN estado varchar(20) NOT NULL DEFAULT 'DEFINITIVA',
    ADD COLUMN fuente varchar(100),
    ADD COLUMN cotizacion_id uuid;

ALTER TABLE valoracion_historica ALTER COLUMN metodo DROP DEFAULT;
ALTER TABLE valoracion_historica ALTER COLUMN estado DROP DEFAULT;

ALTER TABLE valoracion_historica
    ADD CONSTRAINT ck_valoracion_metodo
        CHECK (metodo IN ('COTIZACION_MERCADO', 'ULTIMA_COTIZACION', 'MODELO', 'MANUAL')),
    ADD CONSTRAINT ck_valoracion_estado
        CHECK (estado IN ('PRELIMINAR', 'ESTIMADA', 'DEFINITIVA')),
    ADD CONSTRAINT ck_valoracion_fuente_cotizacion
        CHECK (metodo NOT IN ('COTIZACION_MERCADO', 'ULTIMA_COTIZACION') OR cotizacion_id IS NOT NULL),
    ADD CONSTRAINT fk_valoracion_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES cotizacion (id) ON DELETE RESTRICT;

ALTER TABLE valoracion_historica
    ALTER COLUMN valor_mercado SET NOT NULL,
    ALTER COLUMN valor_costo SET NOT NULL,
    DROP COLUMN ganancia_perdida;

ALTER TABLE valoracion_historica
    ADD COLUMN ganancia_perdida numeric(24,8)
        GENERATED ALWAYS AS (valor_mercado - valor_costo) STORED;

CREATE INDEX idx_valoracion_historica_cotizacion
    ON valoracion_historica (cotizacion_id);

COMMENT ON COLUMN valoracion_historica.metodo
    IS 'Origen del valor: COTIZACION_MERCADO, ULTIMA_COTIZACION, MODELO o MANUAL';
COMMENT ON COLUMN valoracion_historica.estado
    IS 'Grado de confianza: PRELIMINAR, ESTIMADA o DEFINITIVA';
COMMENT ON COLUMN valoracion_historica.cotizacion_id
    IS 'Cotizacion usada cuando el metodo es de mercado o ultima cotizacion';

-- =====================================================================
-- 5. RECOMENDADO - Movimientos, tipos de movimiento y posiciones
-- =====================================================================

-- Trazabilidad de correcciones/reversas (sin borrar el movimiento original).
ALTER TABLE movimiento
    ADD COLUMN movimiento_origen_id uuid,
    ADD COLUMN tipo_cambio_aplicado numeric(20,10);

ALTER TABLE movimiento
    ADD CONSTRAINT fk_movimiento_origen
        FOREIGN KEY (movimiento_origen_id) REFERENCES movimiento (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_movimiento_origen_distinto
        CHECK (movimiento_origen_id IS NULL OR movimiento_origen_id <> id),
    ADD CONSTRAINT ck_movimiento_tipo_cambio
        CHECK (tipo_cambio_aplicado IS NULL OR tipo_cambio_aplicado > 0);

CREATE INDEX idx_movimiento_origen
    ON movimiento (movimiento_origen_id);
CREATE INDEX idx_movimiento_posicion_fecha
    ON movimiento (posicion_id, fecha_operacion DESC);

COMMENT ON COLUMN movimiento.importe
    IS 'Importe bruto en la moneda del movimiento; la comision se registra aparte';
COMMENT ON COLUMN movimiento.tipo_cambio_aplicado
    IS 'Tipo de cambio usado solo si moneda_id difiere de la moneda de la posicion';

-- Naturaleza en el catalogo: evita que la aplicacion deduzca el signo por nombre.
ALTER TABLE tipo_movimiento
    ADD COLUMN naturaleza varchar(20) NOT NULL DEFAULT 'NEUTRA';

ALTER TABLE tipo_movimiento
    ADD CONSTRAINT ck_tipo_movimiento_naturaleza
        CHECK (naturaleza IN ('ENTRADA', 'SALIDA', 'NEUTRA', 'AJUSTE'));

-- No se agrega UNIQUE (cuenta_id, instrumento_id): se permiten varias
-- posiciones del mismo instrumento (lotes, contratos, PPR). Solo un indice
-- de apoyo para consultas y agregaciones.
CREATE INDEX idx_posicion_cuenta_instrumento
    ON posicion (cuenta_id, instrumento_id);

-- Etiqueta opcional para distinguir lotes/contratos dentro de la misma cuenta.
ALTER TABLE posicion ADD COLUMN lote varchar(50);

-- =====================================================================
-- 6. OPCIONAL / PENDIENTE DE APROBACION - PPR y vista de tasa vigente
-- =====================================================================
-- PPR: con el modelo actual ya se puede representar como
--   cuenta_financiera (tipo_cuenta = PPR) -> posicion -> instrumento.
-- plan_ppr solo es necesario si se quieren reglas propias del plan.

CREATE TABLE plan_ppr (
    id                     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cuenta_financiera_id   uuid NOT NULL,
    aportacion_periodica   numeric(24,8),
    periodicidad_aportacion varchar(20),
    limite_deducible_anual numeric(24,8),
    fecha_retiro_estimada  date,
    activo                 boolean NOT NULL DEFAULT true,
    fecha_creacion         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_plan_ppr_cuenta UNIQUE (cuenta_financiera_id),
    CONSTRAINT fk_plan_ppr_cuenta
        FOREIGN KEY (cuenta_financiera_id) REFERENCES cuenta_financiera (id) ON DELETE RESTRICT,
    CONSTRAINT ck_plan_ppr_aportacion
        CHECK (aportacion_periodica IS NULL OR aportacion_periodica > 0),
    CONSTRAINT ck_plan_ppr_limite
        CHECK (limite_deducible_anual IS NULL OR limite_deducible_anual >= 0),
    CONSTRAINT ck_plan_ppr_periodicidad
        CHECK (periodicidad_aportacion IS NULL OR periodicidad_aportacion IN
               ('MENSUAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL', 'UNICA'))
);

-- Tasa vigente observable: ultimo tramo que cubre CURRENT_DATE.
CREATE VIEW v_contrato_tasa_vigente AS
SELECT
    c.id AS contrato_id,
    c.posicion_id,
    c.tipo_tasa,
    h.tasa AS tasa_vigente,
    h.vigencia,
    h.motivo
FROM contrato_inversion c
LEFT JOIN LATERAL (
    SELECT ht.tasa, ht.vigencia, ht.motivo
    FROM historial_tasa ht
    WHERE ht.contrato_id = c.id
      AND ht.vigencia @> CURRENT_DATE
    ORDER BY lower(ht.vigencia) DESC
    LIMIT 1
) h ON true;

COMMIT;

-- =====================================================================
-- Verificaciones sugeridas DESPUES de aplicar (no ejecutadas aqui)
-- =====================================================================
-- \d posicion
-- SELECT conname, contype FROM pg_constraint
--  WHERE conrelid IN ('posicion'::regclass, 'historial_tasa'::regclass);
-- SELECT * FROM v_contrato_tasa_vigente LIMIT 5;
--
-- Prueba de solapamiento (en transaccion y con ROLLBACK):
--   BEGIN;
--   SELECT contrato_id, vigencia FROM historial_tasa LIMIT 1; -- si hubiera datos
--   -- insertar un rango que se traslape debe fallar con exclusion_violation
--   ROLLBACK;
--
-- Reversion: restaurar el dump previo o aplicar el DDL inverso. Con 0 filas,
-- el riesgo de datos es nulo; aun asi, hacer pg_dump antes de aplicar.
