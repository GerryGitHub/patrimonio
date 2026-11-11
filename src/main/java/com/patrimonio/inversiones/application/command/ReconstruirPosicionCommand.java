package com.patrimonio.inversiones.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos para reconstruir y verificar una posición desde su historial.
 */
public record ReconstruirPosicionCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId,
    boolean reparar
) {

    public ReconstruirPosicionCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
    }
}
