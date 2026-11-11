package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import com.patrimonio.inversiones.domain.error.MovimientoNoReversibleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Movimiento confirmado de una posición.
 *
 * <p>Es un agregado inmutable: una vez creado no se modifica ni se elimina. Las
 * correcciones se registran con una {@code REVERSION} que apunta al movimiento
 * original mediante {@code movimientoOrigenId}.</p>
 *
 * <p>Los campos {@code efectoCantidad} y {@code efectoCosto} son los deltas
 * firmados que el motor aplicó a la posición. La posición materializada se
 * actualiza con esos deltas dentro de la misma transacción.</p>
 */
public final class Movimiento {

    private static final int LONGITUD_MAXIMA_OBSERVACIONES = 500;

    private final UUID id;
    private final UUID portafolioId;
    private final UUID cuentaId;
    private final UUID posicionId;
    private final UUID instrumentoId;
    private final TipoMovimiento tipo;
    private final LocalDate fechaOperacion;
    private final LocalDate fechaLiquidacion;
    private final BigDecimal cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal importeBruto;
    private final BigDecimal comisiones;
    private final BigDecimal impuestos;
    private final UUID monedaId;
    private final BigDecimal tipoCambio;
    private final BigDecimal importeNeto;
    private final BigDecimal costoAplicado;
    private final BigDecimal resultadoRealizado;
    private final BigDecimal efectoCantidad;
    private final BigDecimal efectoCosto;
    private final BigDecimal cantidadResultante;
    private final BigDecimal costoResultante;
    private final EstadoMovimiento estado;
    private final UUID usuarioId;
    private final String claveIdempotencia;
    private final UUID movimientoOrigenId;
    private final Optional<String> observaciones;
    private final Instant fechaCreacion;

    private Movimiento(
        UUID id,
        UUID portafolioId,
        UUID cuentaId,
        UUID posicionId,
        UUID instrumentoId,
        TipoMovimiento tipo,
        LocalDate fechaOperacion,
        LocalDate fechaLiquidacion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal importeBruto,
        BigDecimal comisiones,
        BigDecimal impuestos,
        UUID monedaId,
        BigDecimal tipoCambio,
        BigDecimal importeNeto,
        BigDecimal costoAplicado,
        BigDecimal resultadoRealizado,
        BigDecimal efectoCantidad,
        BigDecimal efectoCosto,
        BigDecimal cantidadResultante,
        BigDecimal costoResultante,
        EstadoMovimiento estado,
        UUID usuarioId,
        String claveIdempotencia,
        UUID movimientoOrigenId,
        String observaciones,
        Instant fechaCreacion
    ) {
        this.id = id;
        this.portafolioId = portafolioId;
        this.cuentaId = cuentaId;
        this.posicionId = posicionId;
        this.instrumentoId = instrumentoId;
        this.tipo = tipo;
        this.fechaOperacion = fechaOperacion;
        this.fechaLiquidacion = fechaLiquidacion;
        this.cantidad = escala(cantidad, CalculosMovimiento.ESCALA_CANTIDAD);
        this.precioUnitario = escala(
            precioUnitario, CalculosMovimiento.ESCALA_PRECIO);
        this.importeBruto = escala(
            importeBruto, CalculosMovimiento.ESCALA_IMPORTE);
        this.comisiones = escala(
            comisiones, CalculosMovimiento.ESCALA_IMPORTE);
        this.impuestos = escala(
            impuestos, CalculosMovimiento.ESCALA_IMPORTE);
        this.monedaId = monedaId;
        this.tipoCambio = escala(
            tipoCambio, CalculosMovimiento.ESCALA_IMPORTE);
        this.importeNeto = escala(
            importeNeto, CalculosMovimiento.ESCALA_IMPORTE);
        this.costoAplicado = escala(
            costoAplicado, CalculosMovimiento.ESCALA_IMPORTE);
        this.resultadoRealizado = escala(
            resultadoRealizado, CalculosMovimiento.ESCALA_IMPORTE);
        this.efectoCantidad = escala(
            efectoCantidad, CalculosMovimiento.ESCALA_CANTIDAD);
        this.efectoCosto = escala(
            efectoCosto, CalculosMovimiento.ESCALA_IMPORTE);
        this.cantidadResultante = escala(
            cantidadResultante, CalculosMovimiento.ESCALA_CANTIDAD);
        this.costoResultante = escala(
            costoResultante, CalculosMovimiento.ESCALA_IMPORTE);
        this.estado = estado;
        this.usuarioId = usuarioId;
        this.claveIdempotencia = claveIdempotencia;
        this.movimientoOrigenId = movimientoOrigenId;
        this.observaciones = Optional.ofNullable(observaciones);
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Registra un movimiento nuevo y calcula su efecto sobre la posición.
     *
     * @param solicitud datos validados por el caso de uso
     * @param cantidadActual cantidad materializada antes del movimiento
     * @param costoActual costo total materializado antes del movimiento
     * @param motor motor de cálculo
     * @param ahora instante de creación
     */
    public static Movimiento registrar(
        SolicitudMovimiento solicitud,
        BigDecimal cantidadActual,
        BigDecimal costoActual,
        MotorMovimientos motor,
        Instant ahora
    ) {
        requerir(solicitud, "solicitud");
        requerir(ahora, "ahora");
        requerir(solicitud.id(), "id");
        requerir(solicitud.portafolioId(), "portafolioId");
        requerir(solicitud.cuentaId(), "cuentaId");
        requerir(solicitud.posicionId(), "posicionId");
        requerir(solicitud.instrumentoId(), "instrumentoId");
        requerir(solicitud.monedaId(), "monedaId");
        requerir(solicitud.tipo(), "tipo");
        requerir(solicitud.fechaOperacion(), "fechaOperacion");
        requerir(cantidadActual, "cantidadActual");
        requerir(costoActual, "costoActual");

        if (!solicitud.tipo().operable()) {
            throw new MovimientoInvalidoException(
                "El tipo " + solicitud.tipo()
                    + " no se registra directamente.");
        }
        validarFechaOperacion(solicitud.fechaOperacion(), ahora);
        validarFechaLiquidacion(
            solicitud.fechaLiquidacion(), solicitud.fechaOperacion());
        validarCantidad(solicitud.tipo(), solicitud.cantidad());
        validarPrecio(solicitud.precioUnitario());
        BigDecimal comisiones = nuloCero(solicitud.comisiones());
        BigDecimal impuestos = nuloCero(solicitud.impuestos());
        BigDecimal tipoCambio = solicitud.tipoCambio() == null
            ? BigDecimal.ONE
            : solicitud.tipoCambio();
        validarGastos(comisiones, "comisiones");
        validarGastos(impuestos, "impuestos");
        if (tipoCambio.signum() <= 0) {
            throw new MovimientoInvalidoException(
                "El tipo de cambio debe ser mayor que cero.");
        }
        CalculosMovimiento.validarEscala(
            tipoCambio, CalculosMovimiento.ESCALA_IMPORTE, "tipo de cambio");
        validarObservaciones(solicitud.observaciones());
        if (solicitud.tipo().esAjuste() && solicitud.costoTotalAjuste() == null) {
            throw new MovimientoInvalidoException(
                "Los ajustes deben indicar el costo total ajustado.");
        }

        ResultadoCalculo resultado = motor.calcular(
            solicitud.tipo(),
            new ContextoCalculo(
                cantidadActual,
                costoActual,
                solicitud.cantidad(),
                solicitud.precioUnitario(),
                comisiones,
                impuestos,
                solicitud.costoTotalAjuste(),
                null,
                null,
                null,
                null,
                null,
                null));

        return new Movimiento(
            solicitud.id(),
            solicitud.portafolioId(),
            solicitud.cuentaId(),
            solicitud.posicionId(),
            solicitud.instrumentoId(),
            solicitud.tipo(),
            solicitud.fechaOperacion(),
            solicitud.fechaLiquidacion(),
            solicitud.cantidad(),
            solicitud.precioUnitario(),
            resultado.importeBruto(),
            comisiones,
            impuestos,
            solicitud.monedaId(),
            tipoCambio,
            resultado.importeNeto(),
            resultado.costoAplicado(),
            resultado.resultadoRealizado(),
            resultado.efectoCantidad(),
            resultado.efectoCosto(),
            resultado.cantidadResultante(),
            resultado.costoResultante(),
            EstadoMovimiento.CONFIRMADO,
            solicitud.usuarioId(),
            textoOpcional(solicitud.claveIdempotencia()),
            null,
            solicitud.observaciones(),
            ahora);
    }

    /**
     * Crea la reversión exacta de un movimiento confirmado.
     */
    public static Movimiento reversion(
        Movimiento origen,
        UUID id,
        LocalDate fechaOperacion,
        String observaciones,
        UUID usuarioId,
        String claveIdempotencia,
        BigDecimal cantidadActual,
        BigDecimal costoActual,
        MotorMovimientos motor,
        Instant ahora
    ) {
        requerir(origen, "movimiento original");
        requerir(id, "id");
        requerir(fechaOperacion, "fechaOperacion");
        requerir(cantidadActual, "cantidadActual");
        requerir(costoActual, "costoActual");
        requerir(ahora, "ahora");

        if (origen.tipo == TipoMovimiento.REVERSION) {
            throw new MovimientoNoReversibleException(
                "Una reversión no puede revertirse; registra un movimiento nuevo.");
        }
        if (origen.estado != EstadoMovimiento.CONFIRMADO) {
            throw new MovimientoNoReversibleException(
                "Solo pueden revertirse movimientos confirmados.");
        }
        validarFechaOperacion(fechaOperacion, ahora);
        validarObservaciones(observaciones);

        ResultadoCalculo resultado = motor.calcular(
            TipoMovimiento.REVERSION,
            new ContextoCalculo(
                cantidadActual,
                costoActual,
                origen.cantidad,
                origen.precioUnitario,
                origen.comisiones,
                origen.impuestos,
                null,
                origen.efectoCantidad,
                origen.efectoCosto,
                origen.importeBruto,
                origen.importeNeto,
                origen.costoAplicado,
                origen.resultadoRealizado));

        return new Movimiento(
            id,
            origen.portafolioId,
            origen.cuentaId,
            origen.posicionId,
            origen.instrumentoId,
            TipoMovimiento.REVERSION,
            fechaOperacion,
            null,
            origen.cantidad,
            origen.precioUnitario,
            resultado.importeBruto(),
            origen.comisiones,
            origen.impuestos,
            origen.monedaId,
            origen.tipoCambio,
            resultado.importeNeto(),
            resultado.costoAplicado(),
            resultado.resultadoRealizado(),
            resultado.efectoCantidad(),
            resultado.efectoCosto(),
            resultado.cantidadResultante(),
            resultado.costoResultante(),
            EstadoMovimiento.CONFIRMADO,
            usuarioId,
            textoOpcional(claveIdempotencia),
            origen.id,
            observaciones,
            ahora);
    }

    /**
     * Reconstruye un movimiento desde persistencia. No recalcula efectos: los
     * efectos almacenados son los que produjeron el estado materializado y la
     * reconstrucción los compara contra el historial.
     */
    public static Movimiento reconstruir(
        UUID id,
        UUID portafolioId,
        UUID cuentaId,
        UUID posicionId,
        UUID instrumentoId,
        TipoMovimiento tipo,
        LocalDate fechaOperacion,
        LocalDate fechaLiquidacion,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal importeBruto,
        BigDecimal comisiones,
        BigDecimal impuestos,
        UUID monedaId,
        BigDecimal tipoCambio,
        BigDecimal importeNeto,
        BigDecimal costoAplicado,
        BigDecimal resultadoRealizado,
        BigDecimal efectoCantidad,
        BigDecimal efectoCosto,
        BigDecimal cantidadResultante,
        BigDecimal costoResultante,
        EstadoMovimiento estado,
        UUID usuarioId,
        String claveIdempotencia,
        UUID movimientoOrigenId,
        String observaciones,
        Instant fechaCreacion
    ) {
        requerir(id, "id");
        requerir(portafolioId, "portafolioId");
        requerir(cuentaId, "cuentaId");
        requerir(posicionId, "posicionId");
        requerir(instrumentoId, "instrumentoId");
        requerir(tipo, "tipo");
        requerir(fechaOperacion, "fechaOperacion");
        requerir(cantidad, "cantidad");
        requerir(precioUnitario, "precioUnitario");
        requerir(importeBruto, "importeBruto");
        requerir(comisiones, "comisiones");
        requerir(impuestos, "impuestos");
        requerir(monedaId, "monedaId");
        requerir(tipoCambio, "tipoCambio");
        requerir(importeNeto, "importeNeto");
        requerir(costoAplicado, "costoAplicado");
        requerir(efectoCantidad, "efectoCantidad");
        requerir(efectoCosto, "efectoCosto");
        requerir(cantidadResultante, "cantidadResultante");
        requerir(costoResultante, "costoResultante");
        requerir(estado, "estado");
        requerir(fechaCreacion, "fechaCreacion");
        return new Movimiento(
            id,
            portafolioId,
            cuentaId,
            posicionId,
            instrumentoId,
            tipo,
            fechaOperacion,
            fechaLiquidacion,
            cantidad,
            precioUnitario,
            importeBruto,
            comisiones,
            impuestos,
            monedaId,
            tipoCambio,
            importeNeto,
            costoAplicado,
            resultadoRealizado,
            efectoCantidad,
            efectoCosto,
            cantidadResultante,
            costoResultante,
            estado,
            usuarioId,
            textoOpcional(claveIdempotencia),
            movimientoOrigenId,
            observaciones,
            fechaCreacion);
    }

    private static void validarCantidad(
        TipoMovimiento tipo,
        BigDecimal cantidad
    ) {
        CalculosMovimiento.requerirNoNegativo(cantidad, "cantidad");
        CalculosMovimiento.validarEscala(
            cantidad, CalculosMovimiento.ESCALA_CANTIDAD, "cantidad");
        if (tipo != TipoMovimiento.APERTURA && cantidad.signum() == 0) {
            throw new MovimientoInvalidoException(
                "La cantidad del movimiento debe ser mayor que cero.");
        }
    }

    private static void validarPrecio(BigDecimal precio) {
        CalculosMovimiento.requerirNoNegativo(precio, "precio unitario");
        CalculosMovimiento.validarEscala(
            precio, CalculosMovimiento.ESCALA_PRECIO, "precio unitario");
    }

    private static void validarGastos(BigDecimal valor, String campo) {
        CalculosMovimiento.requerirNoNegativo(valor, campo);
        CalculosMovimiento.validarEscala(
            valor, CalculosMovimiento.ESCALA_IMPORTE, campo);
    }

    private static void validarFechaOperacion(
        LocalDate fechaOperacion,
        Instant ahora
    ) {
        LocalDate hoy = ahora.atZone(ZoneOffset.UTC).toLocalDate();
        if (fechaOperacion.isAfter(hoy)) {
            throw new MovimientoInvalidoException(
                "La fecha de operación no puede ser posterior a hoy.");
        }
    }

    private static void validarFechaLiquidacion(
        LocalDate fechaLiquidacion,
        LocalDate fechaOperacion
    ) {
        if (fechaLiquidacion != null
            && fechaLiquidacion.isBefore(fechaOperacion)) {
            throw new MovimientoInvalidoException(
                "La fecha de liquidación no puede ser anterior a la operación.");
        }
    }

    private static void validarObservaciones(String observaciones) {
        if (observaciones != null
            && observaciones.length() > LONGITUD_MAXIMA_OBSERVACIONES) {
            throw new MovimientoInvalidoException(
                "Las observaciones no pueden superar "
                    + LONGITUD_MAXIMA_OBSERVACIONES + " caracteres.");
        }
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static BigDecimal escala(BigDecimal valor, int escala) {
        return valor == null
            ? null
            : valor.setScale(escala, CalculosMovimiento.REDONDEO);
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static void requerir(Object valor, String campo) {
        if (valor == null) {
            throw new MovimientoInvalidoException(
                "El campo '" + campo + "' es obligatorio.");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID portafolioId() {
        return portafolioId;
    }

    public UUID cuentaId() {
        return cuentaId;
    }

    public UUID posicionId() {
        return posicionId;
    }

    public UUID instrumentoId() {
        return instrumentoId;
    }

    public TipoMovimiento tipo() {
        return tipo;
    }

    public LocalDate fechaOperacion() {
        return fechaOperacion;
    }

    public Optional<LocalDate> fechaLiquidacion() {
        return Optional.ofNullable(fechaLiquidacion);
    }

    public BigDecimal cantidad() {
        return cantidad;
    }

    public BigDecimal precioUnitario() {
        return precioUnitario;
    }

    public BigDecimal importeBruto() {
        return importeBruto;
    }

    public BigDecimal comisiones() {
        return comisiones;
    }

    public BigDecimal impuestos() {
        return impuestos;
    }

    public UUID monedaId() {
        return monedaId;
    }

    public BigDecimal tipoCambio() {
        return tipoCambio;
    }

    public BigDecimal importeNeto() {
        return importeNeto;
    }

    public BigDecimal costoAplicado() {
        return costoAplicado;
    }

    public Optional<BigDecimal> resultadoRealizado() {
        return Optional.ofNullable(resultadoRealizado);
    }

    public BigDecimal efectoCantidad() {
        return efectoCantidad;
    }

    public BigDecimal efectoCosto() {
        return efectoCosto;
    }

    public BigDecimal cantidadResultante() {
        return cantidadResultante;
    }

    public BigDecimal costoResultante() {
        return costoResultante;
    }

    public EstadoMovimiento estado() {
        return estado;
    }

    public Optional<UUID> usuarioId() {
        return Optional.ofNullable(usuarioId);
    }

    public Optional<String> claveIdempotencia() {
        return Optional.ofNullable(claveIdempotencia);
    }

    public Optional<UUID> movimientoOrigenId() {
        return Optional.ofNullable(movimientoOrigenId);
    }

    public Optional<String> observaciones() {
        return observaciones;
    }

    public Instant fechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Resultado que debe aplicarse a la posición materializada. Se deriva de
     * los valores ya calculados y almacenados del movimiento.
     */
    public ResultadoCalculo resultadoAplicable() {
        return new ResultadoCalculo(
            importeBruto,
            importeNeto,
            costoAplicado,
            resultadoRealizado,
            efectoCantidad,
            efectoCosto,
            cantidadResultante,
            costoResultante);
    }

    public boolean esReversion() {
        return tipo == TipoMovimiento.REVERSION;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Movimiento movimiento && id.equals(movimiento.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
