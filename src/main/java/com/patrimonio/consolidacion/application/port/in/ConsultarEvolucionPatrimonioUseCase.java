package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EvolucionView;

/**
 * Caso de uso de la evolución histórica patrimonial.
 */
public interface ConsultarEvolucionPatrimonioUseCase {

    EvolucionView execute(ConsultarPatrimonioQuery query);
}
