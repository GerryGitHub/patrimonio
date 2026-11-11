package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ListarMovimientosQuery;
import com.patrimonio.inversiones.application.view.PaginaMovimientosView;

/**
 * Caso de uso del historial paginado de una posición.
 */
public interface ListarMovimientosUseCase {

    PaginaMovimientosView execute(ListarMovimientosQuery query);
}
