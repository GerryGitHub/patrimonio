package com.patrimonio.inversiones.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta del detalle de un movimiento.
 */
public record ConsultarMovimientoQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID movimientoId
) {

    public ConsultarMovimientoQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(movimientoId, "movimientoId");
    }
}
