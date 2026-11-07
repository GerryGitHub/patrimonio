package com.patrimonio.cuentas.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta del detalle de una cuenta dentro de un portafolio.
 */
public record ConsultarCuentaQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID cuentaId
) {

    public ConsultarCuentaQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
    }
}
