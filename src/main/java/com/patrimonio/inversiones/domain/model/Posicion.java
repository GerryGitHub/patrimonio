package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.PosicionInvalidaException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Posición de un instrumento dentro de una cuenta financiera.
 *
 * <p>La cantidad y el costo son un estado materializado derivado de los
 * movimientos confirmados. No se editan libremente cuando la posición tiene
 * historial. El costo total es la magnitud autoritativa; el costo unitario es
 * una vista calculada como {@code costo total / cantidad}.</p>
 *
 * <p>El costo no es valor de mercado ni incluye rendimiento no realizado.</p>
 */
public final class Posicion {

    private static final int LONGITUD_MAXIMA_OBSERVACIONES = 500;
    private static final int ESCALA_CANTIDAD = 10;
    private static final int ESCALA_COSTO_UNITARIO = 8;
    private static final int ESCALA_COSTO_TOTAL = 10;

    private final UUID id;
    private final UUID cuentaId;
    private final UUID instrumentoId;
    private final BigDecimal cantidad;
    private final BigDecimal costoUnitario;
    private final BigDecimal costoTotal;
    private final BigDecimal resultadoRealizado;
    private final UUID monedaId;
    private final Optional<String> observaciones;
    private final boolean activo;
    private final Instant fechaApertura;
    private final Optional<Instant> fechaUltimoMovimiento;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;
    private final long version;

    private Posicion(
        UUID id,
        UUID cuentaId,
        UUID instrumentoId,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal costoTotal,
        BigDecimal resultadoRealizado,
        UUID monedaId,
        String observaciones,
        boolean activo,
        Instant fechaApertura,
        Instant fechaUltimoMovimiento,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        long version
    ) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.instrumentoId = instrumentoId;
        this.cantidad = escala(cantidad, ESCALA_CANTIDAD);
        this.costoUnitario = escala(costoUnitario, ESCALA_COSTO_UNITARIO);
        this.costoTotal = escala(costoTotal, ESCALA_COSTO_TOTAL);
        this.resultadoRealizado = escala(
            resultadoRealizado == null ? BigDecimal.ZERO : resultadoRealizado,
            ESCALA_COSTO_TOTAL);
        this.monedaId = monedaId;
        this.observaciones = Optional.ofNullable(observaciones);
        this.activo = activo;
        this.fechaApertura = fechaApertura;
        this.fechaUltimoMovimiento = Optional.ofNullable(fechaUltimoMovimiento);
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
        this.version = version;
    }

    /**
     * Crea una posición nueva y activa. El alta genera además su movimiento
     * APERTURA en la capa de aplicación.
     */
    public static Posicion crear(
        UUID id,
        UUID cuentaId,
        UUID instrumentoId,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        UUID monedaId,
        String observaciones,
        Instant fechaApertura,
        Instant ahora
    ) {
        requerir(id, "id");
        requerir(cuentaId, "cuentaId");
        requerir(instrumentoId, "instrumentoId");
        requerir(monedaId, "monedaId");
        requerir(fechaApertura, "fechaApertura");
        requerir(ahora, "ahora");
        validarCantidad(cantidad);
        validarCostoUnitario(costoUnitario);
        validarObservaciones(observaciones);
        validarFechaApertura(fechaApertura, ahora);

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            cantidad,
            costoUnitario,
            costoTotalDe(cantidad, costoUnitario),
            BigDecimal.ZERO,
            monedaId,
            observaciones,
            true,
            fechaApertura,
            fechaApertura,
            ahora,
            ahora,
            0);
    }

    /**
     * Reconstruye una posición desde persistencia. Conserva la firma histórica
     * para los llamadores de la Entrega 2; el mapeo nuevo usa
     * {@link #reconstruirCompleta}.
     */
    public static Posicion reconstruir(
        UUID id,
        UUID cuentaId,
        UUID instrumentoId,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        UUID monedaId,
        String observaciones,
        boolean activo,
        Instant fechaApertura,
        Instant fechaCreacion,
        Instant fechaActualizacion
    ) {
        return reconstruirCompleta(
            id,
            cuentaId,
            instrumentoId,
            cantidad,
            costoUnitario,
            costoTotalDe(cantidad, costoUnitario),
            BigDecimal.ZERO,
            monedaId,
            observaciones,
            activo,
            fechaApertura,
            null,
            fechaCreacion,
            fechaActualizacion,
            0);
    }

    /**
     * Reconstruye el estado materializado completo desde persistencia.
     */
    public static Posicion reconstruirCompleta(
        UUID id,
        UUID cuentaId,
        UUID instrumentoId,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal costoTotal,
        BigDecimal resultadoRealizado,
        UUID monedaId,
        String observaciones,
        boolean activo,
        Instant fechaApertura,
        Instant fechaUltimoMovimiento,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        long version
    ) {
        requerir(id, "id");
        requerir(cuentaId, "cuentaId");
        requerir(instrumentoId, "instrumentoId");
        requerir(monedaId, "monedaId");
        requerir(fechaApertura, "fechaApertura");
        requerir(fechaCreacion, "fechaCreacion");
        requerir(fechaActualizacion, "fechaActualizacion");
        validarCantidad(cantidad);
        validarCostoUnitario(costoUnitario);
        validarCostoTotal(costoTotal);
        validarResultado(resultadoRealizado);
        validarObservaciones(observaciones);
        validarFechaApertura(fechaApertura, fechaActualizacion);
        if (version < 0) {
            throw new PosicionInvalidaException(
                "La versión de la posición no puede ser negativa.");
        }

        BigDecimal costo = costoTotal;
        // Compatibilidad con posiciones insertadas antes de la migración, que
        // no tienen costo_total pero sí cantidad y precio promedio.
        if (costo == null
            || (costo.signum() == 0
                && cantidad.signum() > 0
                && costoUnitario.signum() > 0
                && fechaUltimoMovimiento == null)) {
            costo = costoTotalDe(cantidad, costoUnitario);
        }

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            cantidad,
            costoUnitario,
            costo,
            resultadoRealizado == null ? BigDecimal.ZERO : resultadoRealizado,
            monedaId,
            observaciones,
            activo,
            fechaApertura,
            fechaUltimoMovimiento,
            fechaCreacion,
            fechaActualizacion,
            version);
    }

    /**
     * Devuelve una copia con los datos editables actualizados.
     *
     * <p>Solo debe usarse cuando la posición no tiene movimientos confirmados.
     * El caso de uso lo verifica. Cuando hay historial, la cantidad y el costo
     * se corrigen con movimientos de ajuste o reversión.</p>
     */
    public Posicion actualizarDatos(
        BigDecimal nuevaCantidad,
        BigDecimal nuevoCostoUnitario,
        String nuevasObservaciones,
        Instant nuevaFechaApertura,
        Instant ahora
    ) {
        requerir(nuevaFechaApertura, "fechaApertura");
        requerir(ahora, "ahora");
        validarCantidad(nuevaCantidad);
        validarCostoUnitario(nuevoCostoUnitario);
        validarObservaciones(nuevasObservaciones);
        validarFechaApertura(nuevaFechaApertura, ahora);

        // Si la cantidad y el costo unitario no cambian, se conserva el costo
        // total autoritativo para no introducir un residuo por redondeo del
        // costo unitario derivado.
        BigDecimal nuevoCostoTotal =
            nuevaCantidad.compareTo(cantidad) == 0
                && nuevoCostoUnitario.compareTo(costoUnitario) == 0
                ? costoTotal
                : costoTotalDe(nuevaCantidad, nuevoCostoUnitario);

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            nuevaCantidad,
            nuevoCostoUnitario,
            nuevoCostoTotal,
            resultadoRealizado,
            monedaId,
            nuevasObservaciones,
            activo,
            nuevaFechaApertura,
            fechaUltimoMovimiento.orElse(null),
            fechaCreacion,
            ahora,
            version);
    }

    /**
     * Aplica el resultado calculado de un movimiento confirmado.
     */
    public Posicion aplicar(
        ResultadoCalculo resultado,
        Instant fechaMovimiento,
        Instant ahora
    ) {
        requerir(resultado, "resultado");
        requerir(fechaMovimiento, "fechaMovimiento");
        requerir(ahora, "ahora");
        validarCantidad(resultado.cantidadResultante());
        validarCostoTotal(resultado.costoResultante());

        BigDecimal nuevoResultado = resultadoRealizado.add(
            resultado.resultadoRealizado() == null
                ? BigDecimal.ZERO
                : resultado.resultadoRealizado());
        BigDecimal nuevoCostoUnitario = CalculosMovimiento.costoUnitario(
            resultado.costoResultante(), resultado.cantidadResultante());
        Instant nuevaFechaUltimo = fechaUltimoMovimiento
            .filter(actual -> actual.isAfter(fechaMovimiento))
            .orElse(fechaMovimiento);

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            resultado.cantidadResultante(),
            nuevoCostoUnitario,
            resultado.costoResultante(),
            nuevoResultado,
            monedaId,
            observaciones.orElse(null),
            activo,
            fechaApertura,
            nuevaFechaUltimo,
            fechaCreacion,
            ahora,
            version + 1);
    }

    /**
     * Repara el estado materializado con los valores reconstruidos desde el
     * historial. No modifica movimientos.
     */
    public Posicion reparar(
        BigDecimal cantidadCalculada,
        BigDecimal costoCalculado,
        BigDecimal resultadoCalculado,
        Instant fechaUltimoMovimiento,
        Instant ahora
    ) {
        requerir(ahora, "ahora");
        validarCantidad(cantidadCalculada);
        validarCostoTotal(costoCalculado);
        validarResultado(resultadoCalculado);

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            cantidadCalculada,
            CalculosMovimiento.costoUnitario(
                costoCalculado, cantidadCalculada),
            costoCalculado,
            resultadoCalculado == null ? BigDecimal.ZERO : resultadoCalculado,
            monedaId,
            observaciones.orElse(null),
            activo,
            fechaApertura,
            fechaUltimoMovimiento,
            fechaCreacion,
            ahora,
            version + 1);
    }

    /**
     * Activa o desactiva la posición conservando su historial.
     */
    public Posicion cambiarEstado(boolean nuevoActivo, Instant ahora) {
        requerir(ahora, "ahora");
        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            cantidad,
            costoUnitario,
            costoTotal,
            resultadoRealizado,
            monedaId,
            observaciones.orElse(null),
            nuevoActivo,
            fechaApertura,
            fechaUltimoMovimiento.orElse(null),
            fechaCreacion,
            ahora,
            version);
    }

    /**
     * Costo total de la posición. Es una magnitud de adquisición, no un valor
     * de mercado.
     */
    public BigDecimal costoTotal() {
        return costoTotal;
    }

    private static BigDecimal costoTotalDe(
        BigDecimal cantidad,
        BigDecimal costoUnitario
    ) {
        return cantidad.multiply(costoUnitario).setScale(
            ESCALA_COSTO_TOTAL, RoundingMode.HALF_UP);
    }

    private static BigDecimal escala(BigDecimal valor, int escala) {
        return valor == null
            ? null
            : valor.setScale(escala, RoundingMode.HALF_UP);
    }

    private static void requerir(Object valor, String campo) {
        if (valor == null) {
            throw new PosicionInvalidaException(
                "El campo '" + campo + "' es obligatorio.");
        }
    }

    private static void validarCantidad(BigDecimal cantidad) {
        if (cantidad == null) {
            throw new PosicionInvalidaException(
                "La cantidad de la posición es obligatoria.");
        }
        if (cantidad.signum() < 0) {
            throw new PosicionInvalidaException(
                "La cantidad de la posición no puede ser negativa.");
        }
    }

    private static void validarCostoUnitario(BigDecimal costoUnitario) {
        if (costoUnitario == null) {
            throw new PosicionInvalidaException(
                "El costo unitario de la posición es obligatorio.");
        }
        if (costoUnitario.signum() < 0) {
            throw new PosicionInvalidaException(
                "El costo unitario de la posición no puede ser negativo.");
        }
    }

    private static void validarCostoTotal(BigDecimal costoTotal) {
        if (costoTotal != null && costoTotal.signum() < 0) {
            throw new PosicionInvalidaException(
                "El costo total de la posición no puede ser negativo.");
        }
    }

    private static void validarResultado(BigDecimal resultado) {
        // El resultado realizado puede ser negativo; solo se valida el nulo en
        // la reconstrucción, donde se normaliza a cero.
    }

    private static void validarObservaciones(String observaciones) {
        if (observaciones != null
            && observaciones.length() > LONGITUD_MAXIMA_OBSERVACIONES) {
            throw new PosicionInvalidaException(
                "Las observaciones no pueden superar "
                    + LONGITUD_MAXIMA_OBSERVACIONES + " caracteres.");
        }
    }

    private static void validarFechaApertura(Instant fechaApertura, Instant referencia) {
        if (fechaApertura.isAfter(referencia)) {
            throw new PosicionInvalidaException(
                "La fecha de apertura no puede ser posterior a la fecha actual.");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID cuentaId() {
        return cuentaId;
    }

    public UUID instrumentoId() {
        return instrumentoId;
    }

    public BigDecimal cantidad() {
        return cantidad;
    }

    public BigDecimal costoUnitario() {
        return costoUnitario;
    }

    public UUID monedaId() {
        return monedaId;
    }

    public Optional<String> observaciones() {
        return observaciones;
    }

    public boolean activo() {
        return activo;
    }

    public Instant fechaApertura() {
        return fechaApertura;
    }

    public Optional<Instant> fechaUltimoMovimiento() {
        return fechaUltimoMovimiento;
    }

    public Instant fechaCreacion() {
        return fechaCreacion;
    }

    public Instant fechaActualizacion() {
        return fechaActualizacion;
    }

    public BigDecimal resultadoRealizado() {
        return resultadoRealizado;
    }

    public long version() {
        return version;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Posicion posicion && id.equals(posicion.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
