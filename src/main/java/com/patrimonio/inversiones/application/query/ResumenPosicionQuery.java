package com.patrimonio.inversiones.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta del resumen de saldos de una posición.
 */
public record ResumenPosicionQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId
) {

    public ResumenPosicionQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
    }
}
