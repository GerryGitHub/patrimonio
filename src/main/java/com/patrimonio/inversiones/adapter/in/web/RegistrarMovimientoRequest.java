package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para registrar un movimiento de una posición.
 *
 * <p>Los importes derivados (bruto, neto, costo aplicado y resultado) no se
 * aceptan desde el cliente. La moneda se deriva del instrumento de la posición.
 * Para los ajustes, {@code costoTotalAjuste} es obligatorio y representa la
 * magnitud de costo que se agrega o se retira.</p>
 */
public record RegistrarMovimientoRequest(
    @NotBlank String tipo,
    @PastOrPresent LocalDate fechaOperacion,
    LocalDate fechaLiquidacion,
    @NotNull @DecimalMin("0") BigDecimal cantidad,
    @NotNull @DecimalMin("0") BigDecimal precioUnitario,
    @DecimalMin("0") BigDecimal comisiones,
    @DecimalMin("0") BigDecimal impuestos,
    @DecimalMin(value = "0", inclusive = false) BigDecimal tipoCambio,
    @DecimalMin("0") BigDecimal costoTotalAjuste,
    @Size(max = 500) String observaciones,
    @Size(max = 100) String claveIdempotencia
) {
}
