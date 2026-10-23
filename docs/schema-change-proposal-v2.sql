-- =====================================================================
-- Patrimonio - PROPUESTA v2 de cambios de esquema
-- =====================================================================
-- ESTADO: NO EJECUTADO. Material de revision.
-- La base sigue con 20 tablas, 0 filas y sin extensiones instaladas.
--
-- Motor: PostgreSQL 16.11. Base: patrimonio. Esquema: public.
--
-- CAMBIOS RESPECTO A v1:
--   * btree_gist: dos alternativas explicitas (extension o trigger).
--   * Vigencia [inicio, fin) documentada y validada; fin exclusivo.
--   * Valoraciones: metodos ampliados para acciones/ETF/cripto/plazo/PPR.
--   * Movimientos multidivisa: importe original + convertido + tasa + fecha.
--   * Renovaciones: FK compuesta que exige la misma posicion.
--   * Se elimina el CHECK que obligaba a COMPUESTO al reinvertir.
--   * plan_ppr y la vista quedan SOLO en el bloque opcional (comentado).
--
-- ORDEN DE APLICACION (una sola transaccion):
--   N1. Eliminar la relacion duplicada posicion.contrato_id.
--   N2. Contratos: tasa fija/variable, base de dias, renovacion validada.
--   N3. Historial de tasas: rango de vigencia + no solape.
--   N4. Valoraciones: obligatorios, metodo, estado y ganancia derivada.
--   N5. Movimientos multidivisa.
--   (Opcional, comentado al final)
--
-- ELECCION OBLIGATORIA EN N3 (aplicar UNA sola):
--   A) Con extension btree_gist  -> dejar N3-A activo, N3-B comentado.
--      Requiere superusuario para CREATE EXTENSION. btree_gist 1.7 esta
--      DISPONIBLE en el servidor pero NO instalada.
--   B) Sin extension             -> comentar N3-A, descomentar N3-B
--      (trigger + advisory lock). No requiere superusuario ni extensiones.
--   No aplicar ambas: seria redundante y duplicaria el costo de escritura.
--
-- Antes de aplicar: pg_dump de seguridad y prueba en una base de ensayo.
-- =====================================================================

BEGIN;

-- =====================================================================
-- N1. NECESARIO - Una sola relacion posicion <-> contrato
-- =====================================================================
-- Decision confirmada: una posicion puede tener varios contratos
-- (renovaciones). La unica via es contrato_inversion.posicion_id.
-- Los historicos siguen colgando del contrato correcto.

ALTER TABLE posicion DROP CONSTRAINT fk_posicion_contrato;
DROP INDEX idx_posicion_contrato;
ALTER TABLE posicion DROP COLUMN contrato_id;

-- =====================================================================
-- N2. NECESARIO - Contratos de inversion
-- =====================================================================
-- Proposito: distinguir tasa fija/variable, documentar la base de calculo
-- y validar que una renovacion apunte a un contrato de la MISMA posicion.

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
    ADD CONSTRAINT ck_contrato_origen_distinto
        CHECK (contrato_origen_id IS NULL OR contrato_origen_id <> id);

-- NOTA (correccion del punto 6): NO se agrega el CHECK que obligaba a
-- modalidad COMPUESTO cuando reinvierte_intereses = true. Un contrato de
-- interes simple puede pagar intereses y el inversionista reinvertirlos;
-- son conceptos independientes. Ver docs/schema-change-proposal-v2.md.

-- Renovacion: el contrato origen debe pertenecer a la misma posicion.
-- Se logra con una clave compuesta (id, posicion_id) y una FK compuesta.
ALTER TABLE contrato_inversion
    ADD CONSTRAINT uq_contrato_id_posicion UNIQUE (id, posicion_id),
    ADD CONSTRAINT fk_contrato_origen
        FOREIGN KEY (contrato_origen_id, posicion_id)
        REFERENCES contrato_inversion (id, posicion_id)
        ON DELETE RESTRICT;

CREATE INDEX idx_contrato_inversion_posicion_fecha
    ON contrato_inversion (posicion_id, fecha_inicio DESC);
CREATE INDEX idx_contrato_inversion_origen
    ON contrato_inversion (contrato_origen_id);
DROP INDEX idx_contrato_inversion_posicion;

COMMENT ON COLUMN contrato_inversion.tasa_contractual
    IS 'Tasa pactada (fija) o tasa inicial (variable), decimal anual: 0.0950 = 9.5%';
COMMENT ON COLUMN contrato_inversion.tasa_referencia
    IS 'Indice de referencia para tasa variable (TIIE, SOFR, etc.)';
COMMENT ON COLUMN contrato_inversion.spread
    IS 'Margen sobre la referencia; puede ser negativo';
COMMENT ON COLUMN contrato_inversion.base_calculo_dias
    IS 'Convencion de dias: ACT/360, ACT/365 o 30/360';
COMMENT ON COLUMN contrato_inversion.reinvierte_intereses
    IS 'Los rendimientos en efectivo se reinvierten; NO implica capitalizacion contractual';
COMMENT ON COLUMN contrato_inversion.contrato_origen_id
    IS 'Contrato renovado; la FK compuesta exige la misma posicion';

-- =====================================================================
-- N3. NECESARIO - Historial de tasas por rango de vigencia
-- =====================================================================
-- Semantica: vigencia = [inicio, fin). Inicio incluido, fin EXCLUIDO.
-- Un fin infinito representa la tasa actualmente vigente.
-- Si el negocio dice "vigente hasta el 31/12 inclusive", debe guardarse
-- fin = 2027-01-01. Ver el documento v2, seccion 2.
--
-- La regla [) permite tramos contiguos sin solape:
--   [2026-01-01, 2026-04-01) y [2026-04-01, infinity)
-- son adyacentes, no superpuestos.

ALTER TABLE historial_tasa
    ADD COLUMN vigencia daterange,
    ADD COLUMN fecha_registro timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN motivo varchar(30);

-- Backfill: hoy la tabla tiene 0 filas. Si en el futuro se migraran filas
-- con fecha_fin INCLUSIVA, usar (fecha_fin + 1) como limite exclusivo.
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
        CHECK (
            NOT isempty(vigencia)
            AND NOT lower_inf(vigencia)       -- exige fecha de inicio real
            AND lower_inc(vigencia)           -- inicio incluido
            AND NOT upper_inc(vigencia)       -- fin excluido
        ),
    ADD CONSTRAINT ck_historial_tasa_motivo
        CHECK (motivo IS NULL OR motivo IN ('INICIAL', 'REVISION', 'RENOVACION', 'AJUSTE'));

-- ---------------------------------------------------------------------
-- N3-A. ALTERNATIVA CON EXTENSION (recomendada si se aprueba)
-- ---------------------------------------------------------------------
-- Requiere superusuario. Si no se autoriza, comentar este bloque
-- (desde CREATE EXTENSION hasta el ALTER) y usar N3-B.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE historial_tasa
    ADD CONSTRAINT ex_historial_tasa_sin_solape
        EXCLUDE USING gist (contrato_id WITH =, vigencia WITH &&);

-- ---------------------------------------------------------------------
-- N3-B. ALTERNATIVA SIN EXTENSION (trigger)
-- ---------------------------------------------------------------------
-- Descomentar SOLO si no se aplico N3-A. Usa un advisory lock por contrato
-- para que dos transacciones concurrentes no inserten tramos solapados.
-- Mismo SQLSTATE que una exclusion_violation (23P01).
/*
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
            USING ERRCODE = '23P01',
                  HINT = 'Los tramos de tasa deben ser disjuntos; usar [inicio, fin)';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_historial_tasa_sin_solape
BEFORE INSERT OR UPDATE OF contrato_id, vigencia ON historial_tasa
FOR EACH ROW
EXECUTE FUNCTION fn_historial_tasa_sin_solape();
*/

DROP INDEX idx_historial_tasa_contrato_fecha;
CREATE INDEX idx_historial_tasa_contrato_vigencia
    ON historial_tasa (contrato_id, lower(vigencia) DESC);

COMMENT ON COLUMN historial_tasa.vigencia
    IS 'Intervalo [inicio, fin) de vigencia; upper infinito = tasa vigente';
COMMENT ON COLUMN historial_tasa.fecha_registro
    IS 'Momento de captura del registro (auditoria), no fecha de vigencia';

-- =====================================================================
-- N4. NECESARIO - Valoraciones historicas
-- =====================================================================
-- Proposito: que toda valoracion tenga importes y procedencia auditables.
-- Se amplian los metodos para cubrir activos sin cotizacion de mercado:
--   COTIZACION_MERCADO, ULTIMA_COTIZACION, VALOR_DEVENGADO,
--   PROVEEDOR, MODELO, MANUAL.

ALTER TABLE valoracion_historica
    ADD COLUMN metodo varchar(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN estado varchar(20) NOT NULL DEFAULT 'DEFINITIVA',
    ADD COLUMN fuente varchar(100),
    ADD COLUMN cotizacion_id uuid;

ALTER TABLE valoracion_historica ALTER COLUMN metodo DROP DEFAULT;
ALTER TABLE valoracion_historica ALTER COLUMN estado DROP DEFAULT;

ALTER TABLE valoracion_historica
    ADD CONSTRAINT ck_valoracion_metodo
        CHECK (metodo IN ('COTIZACION_MERCADO', 'ULTIMA_COTIZACION',
                          'VALOR_DEVENGADO', 'PROVEEDOR', 'MODELO', 'MANUAL')),
    ADD CONSTRAINT ck_valoracion_estado
        CHECK (estado IN ('PRELIMINAR', 'ESTIMADA', 'DEFINITIVA')),
    ADD CONSTRAINT ck_valoracion_cotizacion_requerida
        CHECK (metodo NOT IN ('COTIZACION_MERCADO', 'ULTIMA_COTIZACION')
               OR cotizacion_id IS NOT NULL),
    ADD CONSTRAINT ck_valoracion_fuente_proveedor
        CHECK (metodo <> 'PROVEEDOR' OR fuente IS NOT NULL),
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

COMMENT ON COLUMN valoracion_historica.valor_mercado
    IS 'Valor economico de la posicion en moneda_id (mercado o devengado segun metodo)';
COMMENT ON COLUMN valoracion_historica.valor_costo
    IS 'Costo de adquisicion/aportaciones netas, incluidas comisiones atribuibles';
COMMENT ON COLUMN valoracion_historica.metodo
    IS 'COTIZACION_MERCADO, ULTIMA_COTIZACION, VALOR_DEVENGADO, PROVEEDOR, MODELO o MANUAL';
COMMENT ON COLUMN valoracion_historica.cotizacion_id
    IS 'Cotizacion usada; obligatoria para metodos de mercado';

-- =====================================================================
-- N5. NECESARIO - Movimientos multidivisa
-- =====================================================================
-- Convencion:
--   importe            = monto original en moneda_id (con signo de flujo).
--   importe_convertido = mismo flujo en la moneda de la POSICION.
--   tipo_cambio_aplicado = unidades de moneda de la posicion por 1 unidad
--                          de moneda_id.
-- Si la moneda del movimiento es la de la posicion, los tres campos de
-- conversion quedan NULL.
-- La comision sigue siendo un costo separado y no se incluye en importe.

ALTER TABLE movimiento
    ADD COLUMN importe_convertido numeric(24,8),
    ADD COLUMN tipo_cambio_aplicado numeric(20,10),
    ADD COLUMN fecha_tipo_cambio date,
    ADD COLUMN fuente_tipo_cambio varchar(100);

ALTER TABLE movimiento
    ADD CONSTRAINT ck_movimiento_tipo_cambio
        CHECK (tipo_cambio_aplicado IS NULL OR tipo_cambio_aplicado > 0),
    ADD CONSTRAINT ck_movimiento_conversion
        CHECK (
            (importe_convertido IS NULL
                AND tipo_cambio_aplicado IS NULL
                AND fecha_tipo_cambio IS NULL)
            OR
            (importe_convertido IS NOT NULL
                AND tipo_cambio_aplicado IS NOT NULL
                AND fecha_tipo_cambio IS NOT NULL)
        );

COMMENT ON COLUMN movimiento.importe
    IS 'Importe original en moneda_id; signo segun flujo del portafolio';
COMMENT ON COLUMN movimiento.importe_convertido
    IS 'Importe equivalente en la moneda de la posicion; NULL si no hay conversion';
COMMENT ON COLUMN movimiento.tipo_cambio_aplicado
    IS 'Moneda posicion por 1 unidad de moneda_id; NULL si no hay conversion';
COMMENT ON COLUMN movimiento.fecha_tipo_cambio
    IS 'Fecha del tipo de cambio usado (puede no coincidir con la operacion)';

COMMIT;

-- =====================================================================
-- BLOQUE OPCIONAL - NO FORMA PARTE DE LOS CAMBIOS NECESARIOS
-- =====================================================================
-- Descomentar solo lo que se apruebe. plan_ppr y v_contrato_tasa_vigente
-- son explicitamente opcionales y NO se incluyen en el bloque necesario.
/*
BEGIN;

-- O1. Etiqueta de lote para distinguir posiciones del mismo instrumento.
ALTER TABLE posicion ADD COLUMN lote varchar(50);
CREATE INDEX idx_posicion_cuenta_instrumento ON posicion (cuenta_id, instrumento_id);

-- O2. Trazabilidad de correcciones/reversas de movimientos.
ALTER TABLE movimiento ADD COLUMN movimiento_origen_id uuid;
ALTER TABLE movimiento
    ADD CONSTRAINT fk_movimiento_origen
        FOREIGN KEY (movimiento_origen_id) REFERENCES movimiento (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_movimiento_origen_distinto
        CHECK (movimiento_origen_id IS NULL OR movimiento_origen_id <> id);
CREATE INDEX idx_movimiento_origen ON movimiento (movimiento_origen_id);
CREATE INDEX idx_movimiento_posicion_fecha ON movimiento (posicion_id, fecha_operacion DESC);

-- O3. Naturaleza del movimiento en el catalogo.
ALTER TABLE tipo_movimiento ADD COLUMN naturaleza varchar(20) NOT NULL DEFAULT 'NEUTRA';
ALTER TABLE tipo_movimiento
    ADD CONSTRAINT ck_tipo_movimiento_naturaleza
        CHECK (naturaleza IN ('ENTRADA', 'SALIDA', 'NEUTRA', 'AJUSTE'));

-- O4. Plan PPR (solo si se gestionan reglas propias del plan).
CREATE TABLE plan_ppr (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cuenta_financiera_id    uuid NOT NULL,
    aportacion_periodica    numeric(24,8),
    periodicidad_aportacion varchar(20),
    limite_deducible_anual  numeric(24,8),
    fecha_retiro_estimada   date,
    activo                  boolean NOT NULL DEFAULT true,
    fecha_creacion          timestamptz NOT NULL DEFAULT now(),
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

-- O5. Vista de tasa vigente (opcional; no es una tabla ni una restriccion).
CREATE VIEW v_contrato_tasa_vigente AS
SELECT
    c.id AS contrato_id,
    c.posicion_id,
    c.tipo_tasa,
    COALESCE(
        h.tasa,
        CASE WHEN c.tipo_tasa = 'FIJA' THEN c.tasa_contractual END
    ) AS tasa_vigente,
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
*/

-- =====================================================================
-- VERIFICACIONES PARA DESPUES DE LA APROBACION (no ejecutadas aqui)
-- =====================================================================
-- 1. Relacion unica posicion <-> contrato
--    \d posicion
--    SELECT count(*) FROM information_schema.columns
--     WHERE table_name = 'posicion' AND column_name = 'contrato_id';  -- 0
--
-- 2. Renovacion en la misma posicion: debe fallar si el origen es de otra.
--    BEGIN;
--    -- insertar contrato A en la posicion P1 y contrato B en P2 con
--    -- contrato_origen_id = A; debe dar foreign_key_violation.
--    ROLLBACK;
--
-- 3. Historial: tramos contiguos se aceptan; solapados se rechazan.
--    BEGIN;
--    -- [2026-01-01, 2026-04-01) y [2026-04-01, infinity) -> OK
--    -- [2026-01-01, 2026-06-01) y [2026-05-01, 2026-12-31) -> falla
--    ROLLBACK;
--
-- 4. Restriccion de exclusion presente:
--    SELECT conname FROM pg_constraint
--     WHERE conrelid = 'historial_tasa'::regclass AND contype = 'x';
--    (Si se uso N3-B, verificar en su lugar: pg_trigger)
--
-- 5. Valoraciones:
--    SELECT attname, attnotnull, attgenerated
--      FROM pg_attribute
--     WHERE attrelid = 'valoracion_historica'::regclass AND attname IN
--           ('valor_mercado', 'valor_costo', 'ganancia_perdida');
--
-- 6. Movimientos: conversion coherente.
--    \d movimiento
--
-- 7. Integridad general: 0 FK no validadas.
--    SELECT count(*) FROM pg_constraint
--     WHERE contype = 'f' AND NOT convalidated;
