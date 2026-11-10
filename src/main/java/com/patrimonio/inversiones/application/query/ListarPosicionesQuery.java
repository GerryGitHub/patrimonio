package com.patrimonio.inversiones.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de posiciones de un portafolio con filtros opcionales.
 */
public record ListarPosicionesQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID cuentaId,
    UUID instrumentoId,
    UUID claseActivoId,
    Boolean activo
) {

    public ListarPosicionesQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }

    public FiltroPosiciones filtro() {
        return new FiltroPosiciones(cuentaId, instrumentoId, claseActivoId, activo);
    }
}
