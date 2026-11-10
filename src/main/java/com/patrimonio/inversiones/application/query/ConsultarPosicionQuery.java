package com.patrimonio.inversiones.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta del detalle de una posición.
 */
public record ConsultarPosicionQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId
) {

    public ConsultarPosicionQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
    }
}
