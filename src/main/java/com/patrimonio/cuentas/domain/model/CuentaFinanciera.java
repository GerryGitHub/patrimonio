package com.patrimonio.cuentas.domain.model;

import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Cuenta financiera de un portafolio.
 *
 * <p>Agregado de dominio puro: no depende de Spring, JPA, Hibernate ni Jackson.
 * Las referencias a otros agregados ({@code portafolioId}, {@code institucionId},
 * {@code tipoCuentaId}, {@code monedaId}) se mantienen por identificador, sin
 * asociaciones de objetos.</p>
 *
 * <p>Una cuenta no se elimina físicamente: se desactiva y conserva su fecha de
 * cierre. La cuenta puede tener posiciones y contratos en entregables
 * posteriores; este agregado no los anticipa.</p>
 */
public final class CuentaFinanciera {

    private static final int LONGITUD_MAXIMA_NOMBRE = 150;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 500;

    private final UUID id;
    private final UUID portafolioId;
    private final UUID institucionId;
    private final UUID tipoCuentaId;
    private final String nombre;
    private final Optional<String> descripcion;
    private final Optional<NumeroCuenta> numeroCuenta;
    private final UUID monedaId;
    private final boolean activo;
    private final Optional<LocalDate> fechaApertura;
    private final Optional<LocalDate> fechaCierre;

    private CuentaFinanciera(
        UUID id,
        UUID portafolioId,
        UUID institucionId,
        UUID tipoCuentaId,
        String nombre,
        String descripcion,
        NumeroCuenta numeroCuenta,
        UUID monedaId,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        this.id = id;
        this.portafolioId = portafolioId;
        this.institucionId = institucionId;
        this.tipoCuentaId = tipoCuentaId;
        this.nombre = nombre;
        this.descripcion = Optional.ofNullable(descripcion);
        this.numeroCuenta = Optional.ofNullable(numeroCuenta);
        this.monedaId = monedaId;
        this.activo = activo;
        this.fechaApertura = Optional.ofNullable(fechaApertura);
        this.fechaCierre = Optional.ofNullable(fechaCierre);
    }

    /**
     * Crea una cuenta nueva y activa.
     */
    public static CuentaFinanciera crear(
        UUID id,
        UUID portafolioId,
        UUID institucionId,
        UUID tipoCuentaId,
        String nombre,
        String descripcion,
        NumeroCuenta numeroCuenta,
        UUID monedaId,
        LocalDate fechaApertura
    ) {
        requerir(id, "id");
        requerir(portafolioId, "portafolioId");
        requerir(institucionId, "institucionId");
        requerir(tipoCuentaId, "tipoCuentaId");
        requerir(monedaId, "monedaId");
        validarNombre(nombre);
        validarDescripcion(descripcion);
        validarFechas(fechaApertura, null);

        return new CuentaFinanciera(
            id,
            portafolioId,
            institucionId,
            tipoCuentaId,
            nombre,
            descripcion,
            numeroCuenta,
            monedaId,
            true,
            fechaApertura,
            null
        );
    }

    /**
     * Reconstruye una cuenta desde persistencia validando sus invariantes.
     */
    public static CuentaFinanciera reconstruir(
        UUID id,
        UUID portafolioId,
        UUID institucionId,
        UUID tipoCuentaId,
        String nombre,
        String descripcion,
        NumeroCuenta numeroCuenta,
        UUID monedaId,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        requerir(id, "id");
        requerir(portafolioId, "portafolioId");
        requerir(institucionId, "institucionId");
        requerir(tipoCuentaId, "tipoCuentaId");
        requerir(monedaId, "monedaId");
        validarNombre(nombre);
        validarDescripcion(descripcion);
        validarFechas(fechaApertura, fechaCierre);

        return new CuentaFinanciera(
            id,
            portafolioId,
            institucionId,
            tipoCuentaId,
            nombre,
            descripcion,
            numeroCuenta,
            monedaId,
            activo,
            fechaApertura,
            fechaCierre
        );
    }

    /**
     * Sobrecarga de compatibilidad para datos anteriores a la descripción.
     */
    public static CuentaFinanciera reconstruir(
        UUID id,
        UUID portafolioId,
        UUID institucionId,
        UUID tipoCuentaId,
        String nombre,
        NumeroCuenta numeroCuenta,
        UUID monedaId,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        return reconstruir(
            id,
            portafolioId,
            institucionId,
            tipoCuentaId,
            nombre,
            null,
            numeroCuenta,
            monedaId,
            activo,
            fechaApertura,
            fechaCierre);
    }

    /**
     * Devuelve una copia con los datos editables actualizados.
     *
     * <p>El identificador, el portafolio y el estado no cambian por esta vía:
     * el estado tiene su propia operación explícita.</p>
     */
    public CuentaFinanciera actualizarDatos(
        UUID institucionId,
        UUID tipoCuentaId,
        String nombre,
        String descripcion,
        NumeroCuenta numeroCuenta,
        UUID monedaId,
        LocalDate fechaApertura
    ) {
        requerir(institucionId, "institucionId");
        requerir(tipoCuentaId, "tipoCuentaId");
        requerir(monedaId, "monedaId");
        validarNombre(nombre);
        validarDescripcion(descripcion);
        validarFechas(fechaApertura, fechaCierre.orElse(null));

        return new CuentaFinanciera(
            id,
            portafolioId,
            institucionId,
            tipoCuentaId,
            nombre,
            descripcion,
            numeroCuenta,
            monedaId,
            activo,
            fechaApertura,
            fechaCierre.orElse(null)
        );
    }

    /**
     * Cambia el estado de la cuenta.
     *
     * <p>Al desactivar se conserva la fecha de cierre existente o se usa la
     * fecha indicada. Al reactivar se limpia la fecha de cierre.</p>
     */
    public CuentaFinanciera cambiarEstado(boolean nuevoActivo, LocalDate fechaCierre) {
        LocalDate cierre = nuevoActivo
            ? null
            : (fechaCierre != null ? fechaCierre : this.fechaCierre.orElse(null));
        validarFechas(fechaApertura.orElse(null), cierre);

        return new CuentaFinanciera(
            id,
            portafolioId,
            institucionId,
            tipoCuentaId,
            nombre,
            descripcion.orElse(null),
            numeroCuenta.orElse(null),
            monedaId,
            nuevoActivo,
            fechaApertura.orElse(null),
            cierre
        );
    }

    private static void requerir(Object valor, String campo) {
        if (valor == null) {
            throw new CuentaInvalidaException("El campo '" + campo + "' es obligatorio.");
        }
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new CuentaInvalidaException("El nombre de la cuenta es obligatorio.");
        }
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new CuentaInvalidaException(
                "El nombre de la cuenta no puede superar "
                    + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
    }

    private static void validarDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new CuentaInvalidaException(
                "La descripción de la cuenta no puede superar "
                    + LONGITUD_MAXIMA_DESCRIPCION + " caracteres.");
        }
    }

    private static void validarFechas(LocalDate fechaApertura, LocalDate fechaCierre) {
        if (fechaApertura != null && fechaCierre != null
            && fechaCierre.isBefore(fechaApertura)) {
            throw new CuentaInvalidaException(
                "La fecha de cierre no puede ser anterior a la fecha de apertura.");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID portafolioId() {
        return portafolioId;
    }

    public UUID institucionId() {
        return institucionId;
    }

    public UUID tipoCuentaId() {
        return tipoCuentaId;
    }

    public String nombre() {
        return nombre;
    }

    public Optional<String> descripcion() {
        return descripcion;
    }

    public Optional<NumeroCuenta> numeroCuenta() {
        return numeroCuenta;
    }

    public UUID monedaId() {
        return monedaId;
    }

    public boolean activo() {
        return activo;
    }

    public Optional<LocalDate> fechaApertura() {
        return fechaApertura;
    }

    public Optional<LocalDate> fechaCierre() {
        return fechaCierre;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof CuentaFinanciera cuenta && id.equals(cuenta.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
