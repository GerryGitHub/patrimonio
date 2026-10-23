-- =====================================================================
-- Patrimonio - Esquema inicial (Fase 2)
-- Base de datos: patrimonio  |  Motor: PostgreSQL 16
--
-- Documento de referencia del esquema aplicado directamente en la base
-- de datos con:  psql -v ON_ERROR_STOP=1 -f esquema.sql
--
-- Convenciones:
--   * PK: uuid con DEFAULT gen_random_uuid() (integrado en PG >= 13).
--   * Dinero: numeric(24,8); cantidades: numeric(28,10); tasas: numeric(12,8).
--   * Fechas de negocio: date. Marcas de tiempo: timestamptz (UTC).
--   * ON DELETE RESTRICT en todas las FK: no se permite borrado en cascada.
--     La baja de entidades se hace de forma logica con "activo = false".
-- =====================================================================

BEGIN;
SET client_min_messages TO warning;

-- ---------------------------------------------------------------------
-- 1. Catalogos generales
-- ---------------------------------------------------------------------

CREATE TABLE moneda (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo      varchar(10) NOT NULL,
    nombre      varchar(100) NOT NULL,
    simbolo     varchar(10),
    decimales   integer NOT NULL DEFAULT 2,
    activo      boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_moneda_codigo UNIQUE (codigo),
    CONSTRAINT ck_moneda_codigo_formato
        CHECK (codigo = upper(btrim(codigo)) AND length(btrim(codigo)) BETWEEN 3 AND 10),
    CONSTRAINT ck_moneda_decimales CHECK (decimales BETWEEN 0 AND 8)
);
COMMENT ON TABLE moneda IS 'Catalogo de monedas (ISO 4217 y activos digitales)';

CREATE TABLE clase_activo (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      varchar(100) NOT NULL,
    descripcion varchar(255),
    activo      boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_clase_activo_nombre UNIQUE (nombre)
);
COMMENT ON TABLE clase_activo IS 'Catalogo de clases de activo (renta variable, renta fija, etc.)';

CREATE TABLE tipo_instrumento (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    clase_activo_id uuid NOT NULL REFERENCES clase_activo (id) ON DELETE RESTRICT,
    nombre          varchar(100) NOT NULL,
    descripcion     varchar(255),
    activo          boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_tipo_instrumento UNIQUE (clase_activo_id, nombre)
);
COMMENT ON TABLE tipo_instrumento IS 'Tipos de instrumento, siempre dentro de una clase de activo';

CREATE TABLE tipo_cuenta (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      varchar(100) NOT NULL,
    descripcion varchar(255),
    activo      boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_tipo_cuenta_nombre UNIQUE (nombre)
);
COMMENT ON TABLE tipo_cuenta IS 'Catalogo de tipos de cuenta (corriente, corretaje, etc.)';

CREATE TABLE tipo_movimiento (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre           varchar(100) NOT NULL,
    categoria        varchar(50),
    afecta_posicion  boolean NOT NULL DEFAULT false,
    descripcion      varchar(255),
    activo           boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_tipo_movimiento_nombre UNIQUE (nombre)
);
COMMENT ON TABLE tipo_movimiento IS 'Catalogo de tipos de movimiento (compra, venta, dividendo, etc.)';

-- ---------------------------------------------------------------------
-- 2. Usuarios y portafolios
-- ---------------------------------------------------------------------

CREATE TABLE usuario (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre             varchar(150) NOT NULL,
    email              varchar(254) NOT NULL,
    password_hash      varchar(255) NOT NULL,
    activo             boolean NOT NULL DEFAULT true,
    fecha_creacion     timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email CHECK (email ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
);
COMMENT ON COLUMN usuario.password_hash IS 'Hash de la contrasena; nunca la contrasena en claro';
CREATE INDEX idx_usuario_activo ON usuario (activo);

CREATE TABLE portafolio (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre              varchar(150) NOT NULL,
    moneda_base_id      uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    descripcion         varchar(500),
    activo              boolean NOT NULL DEFAULT true,
    fecha_creacion      timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE portafolio IS 'Portafolios de inversion; su moneda base define la valoracion consolidada';
CREATE INDEX idx_portafolio_moneda_base ON portafolio (moneda_base_id);

CREATE TABLE membresia (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id     uuid NOT NULL REFERENCES usuario (id) ON DELETE RESTRICT,
    portafolio_id  uuid NOT NULL REFERENCES portafolio (id) ON DELETE RESTRICT,
    rol            varchar(30) NOT NULL,
    fecha_creacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_membresia_usuario_portafolio UNIQUE (usuario_id, portafolio_id),
    CONSTRAINT ck_membresia_rol
        CHECK (rol IN ('PROPIETARIO', 'ADMINISTRADOR', 'EDITOR', 'LECTOR'))
);
CREATE INDEX idx_membresia_portafolio ON membresia (portafolio_id);

-- ---------------------------------------------------------------------
-- 3. Instituciones y cuentas financieras
-- ---------------------------------------------------------------------

CREATE TABLE institucion (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre     varchar(150) NOT NULL,
    tipo       varchar(50),
    pais       varchar(100),
    sitio_web  varchar(255),
    activo     boolean NOT NULL DEFAULT true
);
CREATE INDEX idx_institucion_nombre ON institucion (nombre);

CREATE TABLE cuenta_financiera (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    portafolio_id  uuid NOT NULL REFERENCES portafolio (id) ON DELETE RESTRICT,
    institucion_id uuid NOT NULL REFERENCES institucion (id) ON DELETE RESTRICT,
    tipo_cuenta_id uuid NOT NULL REFERENCES tipo_cuenta (id) ON DELETE RESTRICT,
    nombre         varchar(150) NOT NULL,
    numero_cuenta  varchar(64),
    moneda_id      uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    activo         boolean NOT NULL DEFAULT true,
    fecha_apertura date,
    fecha_cierre   date,
    CONSTRAINT ck_cuenta_financiera_fechas
        CHECK (fecha_cierre IS NULL OR fecha_apertura IS NULL OR fecha_cierre >= fecha_apertura)
);
CREATE INDEX idx_cuenta_financiera_portafolio ON cuenta_financiera (portafolio_id);
CREATE INDEX idx_cuenta_financiera_institucion ON cuenta_financiera (institucion_id);
CREATE INDEX idx_cuenta_financiera_tipo ON cuenta_financiera (tipo_cuenta_id);
CREATE INDEX idx_cuenta_financiera_moneda ON cuenta_financiera (moneda_id);
-- El numero de cuenta no debe repetirse dentro de una misma institucion.
CREATE UNIQUE INDEX uq_cuenta_financiera_numero
    ON cuenta_financiera (institucion_id, numero_cuenta)
    WHERE numero_cuenta IS NOT NULL;

-- ---------------------------------------------------------------------
-- 4. Instrumentos y sus identificadores
-- ---------------------------------------------------------------------

CREATE TABLE instrumento (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_instrumento_id uuid NOT NULL REFERENCES tipo_instrumento (id) ON DELETE RESTRICT,
    nombre              varchar(200) NOT NULL,
    simbolo             varchar(32),
    emisor              varchar(150),
    moneda_id           uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    descripcion         varchar(500),
    activo              boolean NOT NULL DEFAULT true
);
CREATE INDEX idx_instrumento_tipo ON instrumento (tipo_instrumento_id);
CREATE INDEX idx_instrumento_moneda ON instrumento (moneda_id);
CREATE UNIQUE INDEX uq_instrumento_simbolo_moneda
    ON instrumento (simbolo, moneda_id)
    WHERE simbolo IS NOT NULL;

CREATE TABLE identificador_instrumento (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instrumento_id  uuid NOT NULL REFERENCES instrumento (id) ON DELETE RESTRICT,
    tipo            varchar(50) NOT NULL,
    valor           varchar(100) NOT NULL,
    pais            varchar(100),
    activo          boolean NOT NULL DEFAULT true,
    CONSTRAINT uq_identificador_instrumento_tipo UNIQUE (instrumento_id, tipo)
);
CREATE INDEX idx_identificador_instrumento_valor ON identificador_instrumento (tipo, valor);

-- ---------------------------------------------------------------------
-- 5. Posiciones, movimientos y contratos
-- ---------------------------------------------------------------------

-- posicion se crea sin la FK a contrato_inversion para romper la
-- dependencia circular; la FK se agrega mas abajo con ALTER TABLE.
CREATE TABLE posicion (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cuenta_id           uuid NOT NULL REFERENCES cuenta_financiera (id) ON DELETE RESTRICT,
    instrumento_id      uuid NOT NULL REFERENCES instrumento (id) ON DELETE RESTRICT,
    contrato_id         uuid,
    cantidad            numeric(28,10) NOT NULL DEFAULT 0,
    precio_promedio     numeric(24,8) NOT NULL DEFAULT 0,
    moneda_id           uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    activo              boolean NOT NULL DEFAULT true,
    fecha_apertura      timestamptz NOT NULL DEFAULT now(),
    fecha_actualizacion timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_posicion_precio_promedio CHECK (precio_promedio >= 0),
    CONSTRAINT ck_posicion_fechas CHECK (fecha_actualizacion >= fecha_apertura)
);
CREATE INDEX idx_posicion_cuenta ON posicion (cuenta_id);
CREATE INDEX idx_posicion_instrumento ON posicion (instrumento_id);
CREATE INDEX idx_posicion_moneda ON posicion (moneda_id);

CREATE TABLE contrato_inversion (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    posicion_id          uuid NOT NULL REFERENCES posicion (id) ON DELETE RESTRICT,
    tasa                 numeric(12,8) NOT NULL,
    modalidad_interes    varchar(30) NOT NULL,
    periodicidad_calculo varchar(30),
    periodicidad_pago    varchar(30),
    fecha_inicio         date NOT NULL,
    fecha_vencimiento    date,
    activo               boolean NOT NULL DEFAULT true,
    CONSTRAINT ck_contrato_tasa CHECK (tasa >= 0),
    CONSTRAINT ck_contrato_modalidad CHECK (modalidad_interes IN ('SIMPLE', 'COMPUESTO')),
    CONSTRAINT ck_contrato_fechas
        CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha_inicio)
);
CREATE INDEX idx_contrato_inversion_posicion ON contrato_inversion (posicion_id);

ALTER TABLE posicion
    ADD CONSTRAINT fk_posicion_contrato
    FOREIGN KEY (contrato_id) REFERENCES contrato_inversion (id) ON DELETE RESTRICT;
CREATE INDEX idx_posicion_contrato ON posicion (contrato_id);

CREATE TABLE movimiento (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    posicion_id        uuid NOT NULL REFERENCES posicion (id) ON DELETE RESTRICT,
    tipo_movimiento_id uuid NOT NULL REFERENCES tipo_movimiento (id) ON DELETE RESTRICT,
    fecha_operacion    date NOT NULL,
    cantidad           numeric(28,10) NOT NULL,
    precio             numeric(24,8) NOT NULL,
    importe            numeric(24,8) NOT NULL,
    moneda_id          uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    comision           numeric(20,8) NOT NULL DEFAULT 0,
    descripcion        varchar(500),
    fecha_creacion     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_movimiento_precio CHECK (precio >= 0),
    CONSTRAINT ck_movimiento_comision CHECK (comision >= 0)
);
CREATE INDEX idx_movimiento_posicion ON movimiento (posicion_id);
CREATE INDEX idx_movimiento_tipo ON movimiento (tipo_movimiento_id);
CREATE INDEX idx_movimiento_moneda ON movimiento (moneda_id);
CREATE INDEX idx_movimiento_fecha ON movimiento (fecha_operacion);

-- ---------------------------------------------------------------------
-- 6. Rendimientos (intereses y pagos)
-- ---------------------------------------------------------------------

CREATE TABLE historial_tasa (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    contrato_id  uuid NOT NULL REFERENCES contrato_inversion (id) ON DELETE RESTRICT,
    tasa         numeric(12,8) NOT NULL,
    fecha        date NOT NULL,
    fecha_inicio date,
    fecha_fin    date,
    CONSTRAINT uq_historial_tasa UNIQUE (contrato_id, fecha),
    CONSTRAINT ck_historial_tasa_tasa CHECK (tasa >= 0),
    CONSTRAINT ck_historial_tasa_fechas
        CHECK (fecha_fin IS NULL OR fecha_inicio IS NULL OR fecha_fin >= fecha_inicio)
);
CREATE INDEX idx_historial_tasa_contrato_fecha ON historial_tasa (contrato_id, fecha DESC);

CREATE TABLE pago_rendimiento (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    contrato_id  uuid NOT NULL REFERENCES contrato_inversion (id) ON DELETE RESTRICT,
    fecha_pago   date NOT NULL,
    importe      numeric(24,8) NOT NULL,
    moneda_id    uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    tipo         varchar(50),
    descripcion  varchar(500),
    CONSTRAINT ck_pago_rendimiento_importe CHECK (importe >= 0)
);
CREATE INDEX idx_pago_rendimiento_contrato ON pago_rendimiento (contrato_id);
CREATE INDEX idx_pago_rendimiento_moneda ON pago_rendimiento (moneda_id);

-- ---------------------------------------------------------------------
-- 7. Mercado, valoraciones y tipos de cambio
-- ---------------------------------------------------------------------

CREATE TABLE cotizacion (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    instrumento_id uuid NOT NULL REFERENCES instrumento (id) ON DELETE RESTRICT,
    fecha          date NOT NULL,
    precio         numeric(24,8) NOT NULL,
    moneda_id      uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    fuente         varchar(100),
    CONSTRAINT uq_cotizacion UNIQUE (instrumento_id, fecha, moneda_id),
    CONSTRAINT ck_cotizacion_precio CHECK (precio >= 0)
);
CREATE INDEX idx_cotizacion_instrumento_fecha ON cotizacion (instrumento_id, fecha DESC);
CREATE INDEX idx_cotizacion_moneda ON cotizacion (moneda_id);

CREATE TABLE valoracion_historica (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    posicion_id     uuid NOT NULL REFERENCES posicion (id) ON DELETE RESTRICT,
    fecha           date NOT NULL,
    valor_mercado   numeric(24,8),
    valor_costo     numeric(24,8),
    ganancia_perdida numeric(24,8),
    moneda_id       uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    CONSTRAINT uq_valoracion_historica UNIQUE (posicion_id, fecha)
);
CREATE INDEX idx_valoracion_historica_posicion_fecha
    ON valoracion_historica (posicion_id, fecha DESC);
CREATE INDEX idx_valoracion_historica_moneda ON valoracion_historica (moneda_id);

CREATE TABLE tipo_cambio (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    moneda_origen_id   uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    moneda_destino_id  uuid NOT NULL REFERENCES moneda (id) ON DELETE RESTRICT,
    fecha              date NOT NULL,
    tipo_cambio        numeric(20,10) NOT NULL,
    fuente             varchar(100),
    CONSTRAINT uq_tipo_cambio UNIQUE (moneda_origen_id, moneda_destino_id, fecha),
    CONSTRAINT ck_tipo_cambio_par CHECK (moneda_origen_id <> moneda_destino_id),
    CONSTRAINT ck_tipo_cambio_valor CHECK (tipo_cambio > 0)
);
CREATE INDEX idx_tipo_cambio_destino ON tipo_cambio (moneda_destino_id, fecha DESC);

COMMIT;
