package com.patrimonio.inversiones.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Datos para revertir un movimiento confirmado.
 */
public record RevertirMovimientoCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID movimientoId,
    LocalDate fechaOperacion,
    String observaciones,
    String claveIdempotencia
) {

    public RevertirMovimientoCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(movimientoId, "movimientoId");
    }
}
