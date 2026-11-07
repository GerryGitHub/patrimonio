package com.patrimonio.cuentas.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos del formulario de creación de cuentas financieras.
 *
 * <p>El portafolio se toma de la ruta, nunca del cuerpo. La referencia
 * bancaria es opcional y se conserva completa solo en la persistencia.</p>
 */
public record CrearCuentaRequest(
    @NotNull UUID institucionId,
    @NotNull UUID tipoCuentaId,
    @NotNull UUID monedaId,
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 500) String descripcion,
    @Size(max = 64) String numeroCuenta,
    LocalDate fechaApertura
) {
}
