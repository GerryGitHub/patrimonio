package com.patrimonio.cuentas.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos del formulario de edición de cuentas financieras.
 *
 * <p>El estado no forma parte de la edición: se cambia con su propia
 * operación.</p>
 */
public record ActualizarCuentaRequest(
    @NotNull UUID institucionId,
    @NotNull UUID tipoCuentaId,
    @NotNull UUID monedaId,
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 500) String descripcion,
    @Size(max = 64) String numeroCuenta,
    LocalDate fechaApertura
) {
}
