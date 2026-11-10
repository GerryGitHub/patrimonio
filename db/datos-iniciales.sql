-- =====================================================================
-- Patrimonio - Datos iniciales no destructivos
--
-- Carga los catálogos mínimos que necesitan los formularios de portafolios y
-- cuentas financieras: monedas y tipos de cuenta. El script es idempotente: se
-- puede ejecutar varias veces sobre la misma base sin duplicar filas ni
-- sobrescribir cambios.
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

-- Tipos de cuenta reutilizables por todas las cuentas financieras. La columna
-- nombre tiene una restricción única, así que el alta es idempotente y no
-- sobrescribe descripciones editadas manualmente.
INSERT INTO tipo_cuenta (nombre, descripcion, activo) VALUES
    ('Cuenta corriente',    'Cuenta bancaria de uso diario', true),
    ('Cuenta de ahorro',    'Cuenta bancaria para reservas y ahorro', true),
    ('Cuenta de inversión', 'Cuenta para operar inversiones', true),
    ('Cuenta de corretaje', 'Cuenta de casa de bolsa o bróker', true),
    ('Cuenta de exchange',  'Cuenta en un exchange de activos digitales', true),
    ('Cuenta a plazo',      'Depósito o inversión a plazo fijo', true),
    ('Otra',                'Otro tipo de cuenta financiera', true)
ON CONFLICT (nombre) DO NOTHING;

-- ---------------------------------------------------------------------
-- Clases de activo y tipos de instrumento
--
-- La clasificacion es extensible: una clase nueva solo requiere una fila
-- en clase_activo y sus tipos en tipo_instrumento. Los pagarés y depósitos
-- a plazo de bancos o SOFIPOs NO se modelan aqui: corresponden a contratos
-- de inversión en la siguiente entrega.
-- ---------------------------------------------------------------------
INSERT INTO clase_activo (nombre, descripcion, activo) VALUES
    ('Renta variable',  'Acciones, ETFs y otros instrumentos cotizados', true),
    ('Renta fija',      'Instrumentos de deuda negociables', true),
    ('Activos digitales', 'Criptomonedas y otros activos digitales negociables', true)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO tipo_instrumento (clase_activo_id, nombre, descripcion, activo)
SELECT c.id, v.nombre, v.descripcion, true
FROM (
    VALUES
        ('Renta variable', 'Acción', 'Participación accionaria cotizada'),
        ('Renta variable', 'ETF', 'Fondo cotizado de inversión'),
        ('Renta variable', 'Otro instrumento cotizado', 'Otro instrumento de renta variable'),
        ('Renta fija', 'CETE', 'Certificado de tesorería'),
        ('Renta fija', 'Bono gubernamental', 'Deuda emitida por un gobierno'),
        ('Renta fija', 'Bono corporativo', 'Deuda emitida por una empresa'),
        ('Renta fija', 'Otro instrumento de deuda negociable', 'Otro instrumento de renta fija'),
        ('Activos digitales', 'Criptomoneda', 'Activo digital negociable'),
        ('Activos digitales', 'Otro activo digital negociable', 'Otro activo digital')
) AS v(clase, nombre, descripcion)
JOIN clase_activo c ON c.nombre = v.clase
ON CONFLICT (clase_activo_id, nombre) DO NOTHING;

COMMIT;
