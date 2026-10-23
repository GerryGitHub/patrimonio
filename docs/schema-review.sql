--
-- PostgreSQL database dump
--

\restrict JSFpuYh3SKJkIBXRb1RxoMCMVutS0TUcf9RJCgKFlkjJhgcuL4FdTeHXZKe4bsE

-- Dumped from database version 16.11 (Debian 16.11-1.pgdg13+1)
-- Dumped by pg_dump version 16.11 (Debian 16.11-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: clase_activo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clase_activo (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE clase_activo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.clase_activo IS 'Catalogo de clases de activo (renta variable, renta fija, etc.)';


--
-- Name: contrato_inversion; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: cotizacion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cotizacion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    instrumento_id uuid NOT NULL,
    fecha date NOT NULL,
    precio numeric(24,8) NOT NULL,
    moneda_id uuid NOT NULL,
    fuente character varying(100),
    CONSTRAINT ck_cotizacion_precio CHECK ((precio >= (0)::numeric))
);


--
-- Name: cuenta_financiera; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cuenta_financiera (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    portafolio_id uuid NOT NULL,
    institucion_id uuid NOT NULL,
    tipo_cuenta_id uuid NOT NULL,
    nombre character varying(150) NOT NULL,
    numero_cuenta character varying(64),
    moneda_id uuid NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    fecha_apertura date,
    fecha_cierre date,
    CONSTRAINT ck_cuenta_financiera_fechas CHECK (((fecha_cierre IS NULL) OR (fecha_apertura IS NULL) OR (fecha_cierre >= fecha_apertura)))
);


--
-- Name: historial_tasa; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: identificador_instrumento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.identificador_instrumento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    instrumento_id uuid NOT NULL,
    tipo character varying(50) NOT NULL,
    valor character varying(100) NOT NULL,
    pais character varying(100),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: institucion; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.institucion (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(150) NOT NULL,
    tipo character varying(50),
    pais character varying(100),
    sitio_web character varying(255),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: instrumento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.instrumento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    tipo_instrumento_id uuid NOT NULL,
    nombre character varying(200) NOT NULL,
    simbolo character varying(32),
    emisor character varying(150),
    moneda_id uuid NOT NULL,
    descripcion character varying(500),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: membresia; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.membresia (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    usuario_id uuid NOT NULL,
    portafolio_id uuid NOT NULL,
    rol character varying(30) NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_membresia_rol CHECK (((rol)::text = ANY ((ARRAY['PROPIETARIO'::character varying, 'ADMINISTRADOR'::character varying, 'EDITOR'::character varying, 'LECTOR'::character varying])::text[])))
);


--
-- Name: moneda; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.moneda (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    codigo character varying(10) NOT NULL,
    nombre character varying(100) NOT NULL,
    simbolo character varying(10),
    decimales integer DEFAULT 2 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_moneda_codigo_formato CHECK ((((codigo)::text = upper(btrim((codigo)::text))) AND ((length(btrim((codigo)::text)) >= 3) AND (length(btrim((codigo)::text)) <= 10)))),
    CONSTRAINT ck_moneda_decimales CHECK (((decimales >= 0) AND (decimales <= 8)))
);


--
-- Name: TABLE moneda; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.moneda IS 'Catalogo de monedas (ISO 4217 y activos digitales)';


--
-- Name: movimiento; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: pago_rendimiento; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: portafolio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.portafolio (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(150) NOT NULL,
    moneda_base_id uuid NOT NULL,
    descripcion character varying(500),
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: TABLE portafolio; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.portafolio IS 'Portafolios de inversion; su moneda base define la valoracion consolidada';


--
-- Name: posicion; Type: TABLE; Schema: public; Owner: -
--

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


--
-- Name: tipo_cambio; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_cambio (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    moneda_origen_id uuid NOT NULL,
    moneda_destino_id uuid NOT NULL,
    fecha date NOT NULL,
    tipo_cambio numeric(20,10) NOT NULL,
    fuente character varying(100),
    CONSTRAINT ck_tipo_cambio_par CHECK ((moneda_origen_id <> moneda_destino_id)),
    CONSTRAINT ck_tipo_cambio_valor CHECK ((tipo_cambio > (0)::numeric))
);


--
-- Name: tipo_cuenta; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_cuenta (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE tipo_cuenta; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.tipo_cuenta IS 'Catalogo de tipos de cuenta (corriente, corretaje, etc.)';


--
-- Name: tipo_instrumento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_instrumento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    clase_activo_id uuid NOT NULL,
    nombre character varying(100) NOT NULL,
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE tipo_instrumento; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.tipo_instrumento IS 'Tipos de instrumento, siempre dentro de una clase de activo';


--
-- Name: tipo_movimiento; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_movimiento (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(100) NOT NULL,
    categoria character varying(50),
    afecta_posicion boolean DEFAULT false NOT NULL,
    descripcion character varying(255),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: TABLE tipo_movimiento; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.tipo_movimiento IS 'Catalogo de tipos de movimiento (compra, venta, dividendo, etc.)';


--
-- Name: usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nombre character varying(150) NOT NULL,
    email character varying(254) NOT NULL,
    password_hash character varying(255) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    fecha_creacion timestamp with time zone DEFAULT now() NOT NULL,
    fecha_actualizacion timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT ck_usuario_email CHECK (((email)::text ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'::text))
);


--
-- Name: COLUMN usuario.password_hash; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.usuario.password_hash IS 'Hash de la contrasena; nunca la contrasena en claro';


--
-- Name: valoracion_historica; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.valoracion_historica (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    posicion_id uuid NOT NULL,
    fecha date NOT NULL,
    valor_mercado numeric(24,8),
    valor_costo numeric(24,8),
    ganancia_perdida numeric(24,8),
    moneda_id uuid NOT NULL
);


--
-- Name: clase_activo clase_activo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clase_activo
    ADD CONSTRAINT clase_activo_pkey PRIMARY KEY (id);


--
-- Name: contrato_inversion contrato_inversion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contrato_inversion
    ADD CONSTRAINT contrato_inversion_pkey PRIMARY KEY (id);


--
-- Name: cotizacion cotizacion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizacion
    ADD CONSTRAINT cotizacion_pkey PRIMARY KEY (id);


--
-- Name: cuenta_financiera cuenta_financiera_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuenta_financiera
    ADD CONSTRAINT cuenta_financiera_pkey PRIMARY KEY (id);


--
-- Name: historial_tasa historial_tasa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_tasa
    ADD CONSTRAINT historial_tasa_pkey PRIMARY KEY (id);


--
-- Name: identificador_instrumento identificador_instrumento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identificador_instrumento
    ADD CONSTRAINT identificador_instrumento_pkey PRIMARY KEY (id);


--
-- Name: institucion institucion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.institucion
    ADD CONSTRAINT institucion_pkey PRIMARY KEY (id);


--
-- Name: instrumento instrumento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.instrumento
    ADD CONSTRAINT instrumento_pkey PRIMARY KEY (id);


--
-- Name: membresia membresia_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.membresia
    ADD CONSTRAINT membresia_pkey PRIMARY KEY (id);


--
-- Name: moneda moneda_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moneda
    ADD CONSTRAINT moneda_pkey PRIMARY KEY (id);


--
-- Name: movimiento movimiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimiento
    ADD CONSTRAINT movimiento_pkey PRIMARY KEY (id);


--
-- Name: pago_rendimiento pago_rendimiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pago_rendimiento
    ADD CONSTRAINT pago_rendimiento_pkey PRIMARY KEY (id);


--
-- Name: portafolio portafolio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.portafolio
    ADD CONSTRAINT portafolio_pkey PRIMARY KEY (id);


--
-- Name: posicion posicion_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posicion
    ADD CONSTRAINT posicion_pkey PRIMARY KEY (id);


--
-- Name: tipo_cambio tipo_cambio_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_cambio
    ADD CONSTRAINT tipo_cambio_pkey PRIMARY KEY (id);


--
-- Name: tipo_cuenta tipo_cuenta_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_cuenta
    ADD CONSTRAINT tipo_cuenta_pkey PRIMARY KEY (id);


--
-- Name: tipo_instrumento tipo_instrumento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_instrumento
    ADD CONSTRAINT tipo_instrumento_pkey PRIMARY KEY (id);


--
-- Name: tipo_movimiento tipo_movimiento_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_movimiento
    ADD CONSTRAINT tipo_movimiento_pkey PRIMARY KEY (id);


--
-- Name: clase_activo uq_clase_activo_nombre; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clase_activo
    ADD CONSTRAINT uq_clase_activo_nombre UNIQUE (nombre);


--
-- Name: cotizacion uq_cotizacion; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizacion
    ADD CONSTRAINT uq_cotizacion UNIQUE (instrumento_id, fecha, moneda_id);


--
-- Name: historial_tasa uq_historial_tasa; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_tasa
    ADD CONSTRAINT uq_historial_tasa UNIQUE (contrato_id, fecha);


--
-- Name: identificador_instrumento uq_identificador_instrumento_tipo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identificador_instrumento
    ADD CONSTRAINT uq_identificador_instrumento_tipo UNIQUE (instrumento_id, tipo);


--
-- Name: membresia uq_membresia_usuario_portafolio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.membresia
    ADD CONSTRAINT uq_membresia_usuario_portafolio UNIQUE (usuario_id, portafolio_id);


--
-- Name: moneda uq_moneda_codigo; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.moneda
    ADD CONSTRAINT uq_moneda_codigo UNIQUE (codigo);


--
-- Name: tipo_cambio uq_tipo_cambio; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_cambio
    ADD CONSTRAINT uq_tipo_cambio UNIQUE (moneda_origen_id, moneda_destino_id, fecha);


--
-- Name: tipo_cuenta uq_tipo_cuenta_nombre; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_cuenta
    ADD CONSTRAINT uq_tipo_cuenta_nombre UNIQUE (nombre);


--
-- Name: tipo_instrumento uq_tipo_instrumento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_instrumento
    ADD CONSTRAINT uq_tipo_instrumento UNIQUE (clase_activo_id, nombre);


--
-- Name: tipo_movimiento uq_tipo_movimiento_nombre; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_movimiento
    ADD CONSTRAINT uq_tipo_movimiento_nombre UNIQUE (nombre);


--
-- Name: usuario uq_usuario_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT uq_usuario_email UNIQUE (email);


--
-- Name: valoracion_historica uq_valoracion_historica; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.valoracion_historica
    ADD CONSTRAINT uq_valoracion_historica UNIQUE (posicion_id, fecha);


--
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);


--
-- Name: valoracion_historica valoracion_historica_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.valoracion_historica
    ADD CONSTRAINT valoracion_historica_pkey PRIMARY KEY (id);


--
-- Name: idx_contrato_inversion_posicion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contrato_inversion_posicion ON public.contrato_inversion USING btree (posicion_id);


--
-- Name: idx_cotizacion_instrumento_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cotizacion_instrumento_fecha ON public.cotizacion USING btree (instrumento_id, fecha DESC);


--
-- Name: idx_cotizacion_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cotizacion_moneda ON public.cotizacion USING btree (moneda_id);


--
-- Name: idx_cuenta_financiera_institucion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cuenta_financiera_institucion ON public.cuenta_financiera USING btree (institucion_id);


--
-- Name: idx_cuenta_financiera_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cuenta_financiera_moneda ON public.cuenta_financiera USING btree (moneda_id);


--
-- Name: idx_cuenta_financiera_portafolio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cuenta_financiera_portafolio ON public.cuenta_financiera USING btree (portafolio_id);


--
-- Name: idx_cuenta_financiera_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cuenta_financiera_tipo ON public.cuenta_financiera USING btree (tipo_cuenta_id);


--
-- Name: idx_historial_tasa_contrato_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_historial_tasa_contrato_fecha ON public.historial_tasa USING btree (contrato_id, fecha DESC);


--
-- Name: idx_identificador_instrumento_valor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_identificador_instrumento_valor ON public.identificador_instrumento USING btree (tipo, valor);


--
-- Name: idx_institucion_nombre; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_institucion_nombre ON public.institucion USING btree (nombre);


--
-- Name: idx_instrumento_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instrumento_moneda ON public.instrumento USING btree (moneda_id);


--
-- Name: idx_instrumento_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_instrumento_tipo ON public.instrumento USING btree (tipo_instrumento_id);


--
-- Name: idx_membresia_portafolio; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_membresia_portafolio ON public.membresia USING btree (portafolio_id);


--
-- Name: idx_movimiento_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimiento_fecha ON public.movimiento USING btree (fecha_operacion);


--
-- Name: idx_movimiento_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimiento_moneda ON public.movimiento USING btree (moneda_id);


--
-- Name: idx_movimiento_posicion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimiento_posicion ON public.movimiento USING btree (posicion_id);


--
-- Name: idx_movimiento_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_movimiento_tipo ON public.movimiento USING btree (tipo_movimiento_id);


--
-- Name: idx_pago_rendimiento_contrato; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pago_rendimiento_contrato ON public.pago_rendimiento USING btree (contrato_id);


--
-- Name: idx_pago_rendimiento_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_pago_rendimiento_moneda ON public.pago_rendimiento USING btree (moneda_id);


--
-- Name: idx_portafolio_moneda_base; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_portafolio_moneda_base ON public.portafolio USING btree (moneda_base_id);


--
-- Name: idx_posicion_contrato; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posicion_contrato ON public.posicion USING btree (contrato_id);


--
-- Name: idx_posicion_cuenta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posicion_cuenta ON public.posicion USING btree (cuenta_id);


--
-- Name: idx_posicion_instrumento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posicion_instrumento ON public.posicion USING btree (instrumento_id);


--
-- Name: idx_posicion_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_posicion_moneda ON public.posicion USING btree (moneda_id);


--
-- Name: idx_tipo_cambio_destino; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_tipo_cambio_destino ON public.tipo_cambio USING btree (moneda_destino_id, fecha DESC);


--
-- Name: idx_usuario_activo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_activo ON public.usuario USING btree (activo);


--
-- Name: idx_valoracion_historica_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_valoracion_historica_moneda ON public.valoracion_historica USING btree (moneda_id);


--
-- Name: idx_valoracion_historica_posicion_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_valoracion_historica_posicion_fecha ON public.valoracion_historica USING btree (posicion_id, fecha DESC);


--
-- Name: uq_cuenta_financiera_numero; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_cuenta_financiera_numero ON public.cuenta_financiera USING btree (institucion_id, numero_cuenta) WHERE (numero_cuenta IS NOT NULL);


--
-- Name: uq_instrumento_simbolo_moneda; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_instrumento_simbolo_moneda ON public.instrumento USING btree (simbolo, moneda_id) WHERE (simbolo IS NOT NULL);


--
-- Name: contrato_inversion contrato_inversion_posicion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contrato_inversion
    ADD CONSTRAINT contrato_inversion_posicion_id_fkey FOREIGN KEY (posicion_id) REFERENCES public.posicion(id) ON DELETE RESTRICT;


--
-- Name: cotizacion cotizacion_instrumento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizacion
    ADD CONSTRAINT cotizacion_instrumento_id_fkey FOREIGN KEY (instrumento_id) REFERENCES public.instrumento(id) ON DELETE RESTRICT;


--
-- Name: cotizacion cotizacion_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cotizacion
    ADD CONSTRAINT cotizacion_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: cuenta_financiera cuenta_financiera_institucion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuenta_financiera
    ADD CONSTRAINT cuenta_financiera_institucion_id_fkey FOREIGN KEY (institucion_id) REFERENCES public.institucion(id) ON DELETE RESTRICT;


--
-- Name: cuenta_financiera cuenta_financiera_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuenta_financiera
    ADD CONSTRAINT cuenta_financiera_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: cuenta_financiera cuenta_financiera_portafolio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuenta_financiera
    ADD CONSTRAINT cuenta_financiera_portafolio_id_fkey FOREIGN KEY (portafolio_id) REFERENCES public.portafolio(id) ON DELETE RESTRICT;


--
-- Name: cuenta_financiera cuenta_financiera_tipo_cuenta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cuenta_financiera
    ADD CONSTRAINT cuenta_financiera_tipo_cuenta_id_fkey FOREIGN KEY (tipo_cuenta_id) REFERENCES public.tipo_cuenta(id) ON DELETE RESTRICT;


--
-- Name: posicion fk_posicion_contrato; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posicion
    ADD CONSTRAINT fk_posicion_contrato FOREIGN KEY (contrato_id) REFERENCES public.contrato_inversion(id) ON DELETE RESTRICT;


--
-- Name: historial_tasa historial_tasa_contrato_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.historial_tasa
    ADD CONSTRAINT historial_tasa_contrato_id_fkey FOREIGN KEY (contrato_id) REFERENCES public.contrato_inversion(id) ON DELETE RESTRICT;


--
-- Name: identificador_instrumento identificador_instrumento_instrumento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.identificador_instrumento
    ADD CONSTRAINT identificador_instrumento_instrumento_id_fkey FOREIGN KEY (instrumento_id) REFERENCES public.instrumento(id) ON DELETE RESTRICT;


--
-- Name: instrumento instrumento_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.instrumento
    ADD CONSTRAINT instrumento_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: instrumento instrumento_tipo_instrumento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.instrumento
    ADD CONSTRAINT instrumento_tipo_instrumento_id_fkey FOREIGN KEY (tipo_instrumento_id) REFERENCES public.tipo_instrumento(id) ON DELETE RESTRICT;


--
-- Name: membresia membresia_portafolio_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.membresia
    ADD CONSTRAINT membresia_portafolio_id_fkey FOREIGN KEY (portafolio_id) REFERENCES public.portafolio(id) ON DELETE RESTRICT;


--
-- Name: membresia membresia_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.membresia
    ADD CONSTRAINT membresia_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuario(id) ON DELETE RESTRICT;


--
-- Name: movimiento movimiento_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimiento
    ADD CONSTRAINT movimiento_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: movimiento movimiento_posicion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimiento
    ADD CONSTRAINT movimiento_posicion_id_fkey FOREIGN KEY (posicion_id) REFERENCES public.posicion(id) ON DELETE RESTRICT;


--
-- Name: movimiento movimiento_tipo_movimiento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.movimiento
    ADD CONSTRAINT movimiento_tipo_movimiento_id_fkey FOREIGN KEY (tipo_movimiento_id) REFERENCES public.tipo_movimiento(id) ON DELETE RESTRICT;


--
-- Name: pago_rendimiento pago_rendimiento_contrato_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pago_rendimiento
    ADD CONSTRAINT pago_rendimiento_contrato_id_fkey FOREIGN KEY (contrato_id) REFERENCES public.contrato_inversion(id) ON DELETE RESTRICT;


--
-- Name: pago_rendimiento pago_rendimiento_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pago_rendimiento
    ADD CONSTRAINT pago_rendimiento_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: portafolio portafolio_moneda_base_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.portafolio
    ADD CONSTRAINT portafolio_moneda_base_id_fkey FOREIGN KEY (moneda_base_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: posicion posicion_cuenta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posicion
    ADD CONSTRAINT posicion_cuenta_id_fkey FOREIGN KEY (cuenta_id) REFERENCES public.cuenta_financiera(id) ON DELETE RESTRICT;


--
-- Name: posicion posicion_instrumento_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posicion
    ADD CONSTRAINT posicion_instrumento_id_fkey FOREIGN KEY (instrumento_id) REFERENCES public.instrumento(id) ON DELETE RESTRICT;


--
-- Name: posicion posicion_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.posicion
    ADD CONSTRAINT posicion_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: tipo_cambio tipo_cambio_moneda_destino_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_cambio
    ADD CONSTRAINT tipo_cambio_moneda_destino_id_fkey FOREIGN KEY (moneda_destino_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: tipo_cambio tipo_cambio_moneda_origen_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_cambio
    ADD CONSTRAINT tipo_cambio_moneda_origen_id_fkey FOREIGN KEY (moneda_origen_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: tipo_instrumento tipo_instrumento_clase_activo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_instrumento
    ADD CONSTRAINT tipo_instrumento_clase_activo_id_fkey FOREIGN KEY (clase_activo_id) REFERENCES public.clase_activo(id) ON DELETE RESTRICT;


--
-- Name: valoracion_historica valoracion_historica_moneda_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.valoracion_historica
    ADD CONSTRAINT valoracion_historica_moneda_id_fkey FOREIGN KEY (moneda_id) REFERENCES public.moneda(id) ON DELETE RESTRICT;


--
-- Name: valoracion_historica valoracion_historica_posicion_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.valoracion_historica
    ADD CONSTRAINT valoracion_historica_posicion_id_fkey FOREIGN KEY (posicion_id) REFERENCES public.posicion(id) ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

\unrestrict JSFpuYh3SKJkIBXRb1RxoMCMVutS0TUcf9RJCgKFlkjJhgcuL4FdTeHXZKe4bsE

