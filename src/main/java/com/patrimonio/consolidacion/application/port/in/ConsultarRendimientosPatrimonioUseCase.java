package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.RendimientosView;

/**
 * Caso de uso de rendimientos y costos de operación.
 */
public interface ConsultarRendimientosPatrimonioUseCase {

    RendimientosView rendimientos(ConsultarPatrimonioQuery query);
}
