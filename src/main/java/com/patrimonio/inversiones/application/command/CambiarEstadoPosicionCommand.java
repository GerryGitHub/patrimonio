package com.patrimonio.inversiones.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos para activar o desactivar una posición.
 */
public record CambiarEstadoPosicionCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId,
    boolean activo
) {

    public CambiarEstadoPosicionCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
    }
}
