package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para corregir una posición. La cuenta y el instrumento no cambian.
 */
public record ActualizarPosicionRequest(
    @NotNull @DecimalMin("0") BigDecimal cantidad,
    @NotNull @DecimalMin("0") BigDecimal costoUnitario,
    @PastOrPresent LocalDate fechaApertura,
    @Size(max = 500) String observaciones
) {
}
