package com.patrimonio.cuentas.application.view;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de aplicación de una cuenta financiera.
 *
 * <p>Los campos opcionales del esquema se representan con {@code null}. El
 * número de cuenta viaja completo dentro de la aplicación; el adaptador HTTP
 * lo enmascara antes de serializarlo.</p>
 */
public record CuentaFinancieraView(
    UUID id,
    String nombre,
    String numeroCuenta,
    UUID institucionId,
    UUID tipoCuentaId,
    UUID monedaId,
    boolean activo,
    LocalDate fechaApertura,
    LocalDate fechaCierre,
    String descripcion
) {

    /**
     * Constructor de compatibilidad para consumidores anteriores a la
     * descripción.
     */
    public CuentaFinancieraView(
        UUID id,
        String nombre,
        String numeroCuenta,
        UUID institucionId,
        UUID tipoCuentaId,
        UUID monedaId,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        this(
            id,
            nombre,
            numeroCuenta,
            institucionId,
            tipoCuentaId,
            monedaId,
            activo,
            fechaApertura,
            fechaCierre,
            null);
    }
}
