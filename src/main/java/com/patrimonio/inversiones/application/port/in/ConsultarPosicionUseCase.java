package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ConsultarPosicionQuery;
import com.patrimonio.inversiones.application.view.PosicionView;

/**
 * Caso de uso: consultar el detalle de una posición.
 */
public interface ConsultarPosicionUseCase {

    PosicionView execute(ConsultarPosicionQuery query);
}
