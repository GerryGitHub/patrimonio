package com.patrimonio.cuentas.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de las cuentas financieras de un portafolio.
 *
 * <p>Incluye el usuario autenticado porque la autorización por membresía se
 * comprueba dentro del caso de uso, no en el controlador.</p>
 */
public record ListarCuentasPorPortafolioQuery(
    UUID usuarioId,
    UUID portafolioId
) {

    public ListarCuentasPorPortafolioQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
