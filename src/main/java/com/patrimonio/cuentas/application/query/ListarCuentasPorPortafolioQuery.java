package com.patrimonio.cuentas.application.query;

import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de las cuentas financieras de un portafolio.
 *
 * <p>Incluye el usuario autenticado porque la autorización por membresía se
 * comprueba dentro del caso de uso, no en el controlador. Los filtros son
 * opcionales.</p>
 */
public record ListarCuentasPorPortafolioQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID institucionId,
    UUID tipoCuentaId,
    Boolean activo
) {

    public ListarCuentasPorPortafolioQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }

    /**
     * Constructor de compatibilidad sin filtros.
     */
    public ListarCuentasPorPortafolioQuery(UUID usuarioId, UUID portafolioId) {
        this(usuarioId, portafolioId, null, null, null);
    }

    public FiltroCuentas filtro() {
        return new FiltroCuentas(institucionId, tipoCuentaId, activo);
    }
}
