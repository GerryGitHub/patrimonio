package com.patrimonio.consolidacion.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de snapshots manuales de un portafolio autorizado.
 */
public record ConsultarSnapshotsQuery(
    UUID usuarioId,
    UUID portafolioId
) {

    public ConsultarSnapshotsQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
