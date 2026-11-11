package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ResumenPosicionQuery;
import com.patrimonio.inversiones.application.view.ResumenPosicionView;

/**
 * Caso de uso del resumen de saldos de una posición.
 */
public interface ResumenPosicionUseCase {

    ResumenPosicionView execute(ResumenPosicionQuery query);
}
