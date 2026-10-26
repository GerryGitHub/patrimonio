package com.patrimonio.cuentas.application.view;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de aplicación de una cuenta financiera.
 *
 * <p>Los campos {@code numeroCuenta}, {@code fechaApertura} y {@code fechaCierre}
 * pueden ser {@code null} porque las columnas correspondientes admiten NULL en
 * el esquema.</p>
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
    LocalDate fechaCierre
) {
}
