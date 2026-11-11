package com.patrimonio.inversiones.application.command;

import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Datos para registrar un movimiento de una posición.
 *
 * <p>El importe bruto, el neto, el costo aplicado y el resultado realizado no
 * viajan en el comando: los calcula el dominio. Para los ajustes, el cliente
 * debe enviar {@code costoTotalAjuste} de forma explícita.</p>
 */
public record RegistrarMovimientoCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId,
    TipoMovimiento tipo,
    LocalDate fechaOperacion,
    LocalDate fechaLiquidacion,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal comisiones,
    BigDecimal impuestos,
    BigDecimal tipoCambio,
    BigDecimal costoTotalAjuste,
    String observaciones,
    String claveIdempotencia
) {

    public RegistrarMovimientoCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
        Objects.requireNonNull(tipo, "tipo");
    }
}
