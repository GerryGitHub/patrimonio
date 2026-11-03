package com.patrimonio.acceso.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de los portafolios a los que un usuario tiene acceso.
 */
public record ListarPortafoliosAutorizadosQuery(UUID usuarioId) {

    public ListarPortafoliosAutorizadosQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
    }
}
