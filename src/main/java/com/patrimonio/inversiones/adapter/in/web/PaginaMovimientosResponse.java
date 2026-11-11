package com.patrimonio.inversiones.adapter.in.web;

import java.util.List;

/**
 * Página del historial de movimientos de una posición.
 */
public record PaginaMovimientosResponse(
    List<MovimientoResponse> items,
    int pagina,
    int tamano,
    long total,
    int totalPaginas
) {
}
