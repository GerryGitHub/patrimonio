package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ResumenView;

/**
 * Caso de uso del resumen patrimonial.
 */
public interface ConsultarResumenPatrimonioUseCase {

    ResumenView resumen(ConsultarPatrimonioQuery query);
}
