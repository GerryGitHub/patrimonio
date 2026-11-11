package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Datos para revertir un movimiento confirmado.
 */
public record RevertirMovimientoRequest(
    @PastOrPresent LocalDate fechaOperacion,
    @Size(max = 500) String observaciones,
    @Size(max = 100) String claveIdempotencia
) {
}
