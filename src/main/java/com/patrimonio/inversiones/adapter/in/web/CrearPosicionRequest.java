package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos para registrar la posición inicial de un instrumento en una cuenta.
 *
 * <p>La moneda no se envía: se deriva del instrumento. La fecha es opcional;
 * si se omite se usa la fecha actual.</p>
 */
public record CrearPosicionRequest(
    @NotNull UUID cuentaId,
    @NotNull UUID instrumentoId,
    @NotNull @DecimalMin("0") BigDecimal cantidad,
    @NotNull @DecimalMin("0") BigDecimal costoUnitario,
    @PastOrPresent LocalDate fechaApertura,
    @Size(max = 500) String observaciones
) {
}
