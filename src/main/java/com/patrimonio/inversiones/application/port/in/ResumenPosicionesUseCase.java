package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ResumenPosicionesQuery;
import com.patrimonio.inversiones.application.view.ResumenPosicionesView;

/**
 * Caso de uso: calcular los agregados de posiciones de un portafolio.
 */
public interface ResumenPosicionesUseCase {

    ResumenPosicionesView execute(ResumenPosicionesQuery query);
}
