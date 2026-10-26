package com.patrimonio.cuentas.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de las cuentas financieras de un portafolio.
 */
public record ListarCuentasPorPortafolioQuery(UUID portafolioId) {

    public ListarCuentasPorPortafolioQuery {
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
