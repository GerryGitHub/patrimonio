package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CentroView;

/**
 * Caso de uso del Centro de Patrimonio consolidado.
 */
public interface ConsultarCentroPatrimonioUseCase {

    CentroView centro(ConsultarPatrimonioQuery query);
}
