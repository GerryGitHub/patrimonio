package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ListarPosicionesQuery;
import com.patrimonio.inversiones.application.view.PosicionView;
import java.util.List;

/**
 * Caso de uso: listar las posiciones de un portafolio.
 */
public interface ListarPosicionesUseCase {

    List<PosicionView> execute(ListarPosicionesQuery query);
}
