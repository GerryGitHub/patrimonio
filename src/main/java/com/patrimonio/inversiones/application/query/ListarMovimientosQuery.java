package com.patrimonio.inversiones.application.query;

import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta paginada del historial de una posición.
 */
public record ListarMovimientosQuery(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId,
    TipoMovimiento tipo,
    LocalDate desde,
    LocalDate hasta,
    int pagina,
    int tamano,
    boolean descendente
) {

    public static final int TAMANO_MAXIMO = 100;

    public ListarMovimientosQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new IllegalArgumentException(
                "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
    }

    public FiltroMovimientos filtro() {
        return new FiltroMovimientos(tipo, desde, hasta);
    }
}
