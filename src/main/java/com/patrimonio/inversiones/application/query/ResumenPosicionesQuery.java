package com.patrimonio.inversiones.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de los agregados de posiciones de un portafolio.
 */
public record ResumenPosicionesQuery(
    UUID usuarioId,
    UUID portafolioId
) {

    public ResumenPosicionesQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
