-- =====================================================================
-- Patrimonio - Cambios de esquema de la Entrega 2
--
-- Catalogo de instrumentos y posiciones iniciales.
--
-- Este script es ADITIVO e IDEMPOTENTE: se puede aplicar sobre una base
-- existente creada con db/esquema.sql sin eliminar ni reescribir datos.
-- Para instalaciones nuevas, db/esquema.sql ya incluye estas columnas.
--
-- Uso:
--   psql -v ON_ERROR_STOP=1 -f db/esquema-entrega2.sql
--   psql -v ON_ERROR_STOP=1 -f db/datos-iniciales.sql
--
-- No introduce Flyway ni cambios destructivos. Las columnas nuevas son
-- opcionales o tienen valor por defecto, por lo que las filas existentes
-- siguen siendo validas.
-- =====================================================================

BEGIN;
SET client_min_messages TO warning;

-- ---------------------------------------------------------------------
-- 1. Instrumento: mercado, identificador externo y fechas de auditoria
-- ---------------------------------------------------------------------
ALTER TABLE instrumento
    ADD COLUMN IF NOT EXISTS mercado varchar(100),
    ADD COLUMN IF NOT EXISTS identificador_externo varchar(100),
    ADD COLUMN IF NOT EXISTS fecha_creacion timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS fecha_actualizacion timestamptz NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_instrumento_mercado
    ON instrumento (mercado);

-- El ISIN es unico a nivel global mientras el identificador este activo.
CREATE UNIQUE INDEX IF NOT EXISTS uq_identificador_instrumento_isin
    ON identificador_instrumento (valor)
    WHERE tipo = 'ISIN' AND activo;

-- ---------------------------------------------------------------------
-- 2. Posicion: observaciones, fecha de creacion e indice de agregacion
-- ---------------------------------------------------------------------
ALTER TABLE posicion
    ADD COLUMN IF NOT EXISTS observaciones varchar(500),
    ADD COLUMN IF NOT EXISTS fecha_creacion timestamptz NOT NULL DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_posicion_cuenta_instrumento
    ON posicion (cuenta_id, instrumento_id);

-- La cantidad inicial de una posicion bursatil no puede ser negativa.
-- Se agrega solo si no existe para que el script sea reejecutable.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_posicion_cantidad'
          AND conrelid = 'posicion'::regclass
    ) THEN
        ALTER TABLE posicion
            ADD CONSTRAINT ck_posicion_cantidad CHECK (cantidad >= 0);
    END IF;
END
$$;

COMMIT;
