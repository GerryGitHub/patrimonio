-- =====================================================================
-- Patrimonio - Datos iniciales no destructivos
--
-- Carga el catálogo mínimo de monedas que necesita el formulario de
-- portafolios. El script es idempotente: se puede ejecutar varias veces
-- sobre la misma base sin duplicar filas ni sobrescribir cambios.
--
-- Uso:
--   psql -v ON_ERROR_STOP=1 -f db/esquema.sql
--   psql -v ON_ERROR_STOP=1 -f db/datos-iniciales.sql
--
-- No crea usuarios, portafolios ni cuentas. Esas entidades se crean desde
-- la aplicación o con los scripts de pruebas manuales.
-- =====================================================================

BEGIN;

INSERT INTO moneda (codigo, nombre, simbolo, decimales, activo) VALUES
    ('USD', 'Dólar estadounidense', '$',   2, true),
    ('EUR', 'Euro',                 '€',   2, true),
    ('GBP', 'Libra esterlina',      '£',   2, true),
    ('JPY', 'Yen japonés',          '¥',   0, true),
    ('CHF', 'Franco suizo',         'CHF', 2, true),
    ('CAD', 'Dólar canadiense',     'C$',  2, true),
    ('MXN', 'Peso mexicano',        '$',   2, true),
    ('COP', 'Peso colombiano',      '$',   2, true),
    ('CLP', 'Peso chileno',         '$',   0, true),
    ('PEN', 'Sol peruano',          'S/',  2, true),
    ('ARS', 'Peso argentino',       '$',   2, true),
    ('BRL', 'Real brasileño',       'R$',  2, true)
ON CONFLICT (codigo) DO NOTHING;

COMMIT;
