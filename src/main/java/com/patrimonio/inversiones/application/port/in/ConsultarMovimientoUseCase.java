package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ConsultarMovimientoQuery;
import com.patrimonio.inversiones.application.view.MovimientoView;

/**
 * Caso de uso de consulta de un movimiento.
 */
public interface ConsultarMovimientoUseCase {

    MovimientoView execute(ConsultarMovimientoQuery query);
}
