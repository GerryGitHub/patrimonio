package com.patrimonio.inversiones.application.view;

import java.util.List;

/**
 * Página de movimientos de una posición.
 */
public record PaginaMovimientosView(
    List<MovimientoView> items,
    int pagina,
    int tamano,
    long total,
    int totalPaginas
) {
}
