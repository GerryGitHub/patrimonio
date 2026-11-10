package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.PosicionInvalidaException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Posición inicial de un instrumento dentro de una cuenta financiera.
 *
 * <p>Agregado de dominio puro. La posición pertenece a la cuenta y referencia
 * el instrumento y la moneda por identificador. Una cuenta puede tener varias
 * posiciones y un instrumento puede existir en varias cuentas.</p>
 *
 * <p>En esta entrega la posición se registra con cantidad, costo unitario y
 * fecha de apertura. No se implementan todavía compras, ventas, movimientos ni
 * valuaciones de mercado. El costo total es el único importe derivado y se
 * calcula como {@code cantidad × costo unitario}; no representa valor de
 * mercado ni rendimiento.</p>
 */
public final class Posicion {

    private static final int LONGITUD_MAXIMA_OBSERVACIONES = 500;
    private static final int ESCALA_COSTO_TOTAL = 10;

    private final UUID id;
    private final UUID cuentaId;
    private final UUID instrumentoId;
    private final BigDecimal cantidad;
    private final BigDecimal costoUnitario;
    private final UUID monedaId;
    private final Optional<String> observaciones;
    private final boolean activo;
    private final Instant fechaApertura;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;

    private Posicion(
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
        this.id = id;
        this.cuentaId = cuentaId;
        this.instrumentoId = instrumentoId;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.monedaId = monedaId;
        this.observaciones = Optional.ofNullable(observaciones);
        this.activo = activo;
        this.fechaApertura = fechaApertura;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    /**
     * Crea una posición nueva y activa.
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
            monedaId,
            observaciones,
            true,
            fechaApertura,
            ahora,
            ahora
        );
    }

    /**
     * Reconstruye una posición desde persistencia validando sus invariantes.
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
        requerir(id, "id");
        requerir(cuentaId, "cuentaId");
        requerir(instrumentoId, "instrumentoId");
        requerir(monedaId, "monedaId");
        requerir(fechaApertura, "fechaApertura");
        requerir(fechaCreacion, "fechaCreacion");
        requerir(fechaActualizacion, "fechaActualizacion");
        validarCantidad(cantidad);
        validarCostoUnitario(costoUnitario);
        validarObservaciones(observaciones);
        validarFechaApertura(fechaApertura, fechaActualizacion);

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            cantidad,
            costoUnitario,
            monedaId,
            observaciones,
            activo,
            fechaApertura,
            fechaCreacion,
            fechaActualizacion
        );
    }

    /**
     * Devuelve una copia con los datos editables actualizados.
     *
     * <p>Mientras no exista el motor de movimientos, la cantidad y el costo
     * unitario se pueden corregir. Cuando se implementen los movimientos,
     * estos campos quedarán derivados de las operaciones y esta edición se
     * restringirá.</p>
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

        return new Posicion(
            id,
            cuentaId,
            instrumentoId,
            nuevaCantidad,
            nuevoCostoUnitario,
            monedaId,
            nuevasObservaciones,
            activo,
            nuevaFechaApertura,
            fechaCreacion,
            ahora
        );
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
            monedaId,
            observaciones.orElse(null),
            nuevoActivo,
            fechaApertura,
            fechaCreacion,
            ahora
        );
    }

    /**
     * Costo total de la posición.
     *
     * <p>Es {@code cantidad × costo unitario}. No es valor de mercado, no
     * incluye rendimiento y no se actualiza con precios externos.</p>
     */
    public BigDecimal costoTotal() {
        return cantidad.multiply(costoUnitario).setScale(
            ESCALA_COSTO_TOTAL, RoundingMode.HALF_UP);
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

    public Instant fechaCreacion() {
        return fechaCreacion;
    }

    public Instant fechaActualizacion() {
        return fechaActualizacion;
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
