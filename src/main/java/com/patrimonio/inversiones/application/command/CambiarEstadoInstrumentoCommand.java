package com.patrimonio.inversiones.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos para activar o desactivar un instrumento.
 */
public record CambiarEstadoInstrumentoCommand(
    UUID usuarioId,
    UUID instrumentoId,
    boolean activo
) {

    public CambiarEstadoInstrumentoCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(instrumentoId, "instrumentoId");
    }
}
