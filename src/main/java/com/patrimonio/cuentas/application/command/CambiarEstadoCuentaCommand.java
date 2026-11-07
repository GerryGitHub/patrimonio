package com.patrimonio.cuentas.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos para activar o desactivar una cuenta financiera.
 */
public record CambiarEstadoCuentaCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID cuentaId,
    boolean activo
) {

    public CambiarEstadoCuentaCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
    }
}
