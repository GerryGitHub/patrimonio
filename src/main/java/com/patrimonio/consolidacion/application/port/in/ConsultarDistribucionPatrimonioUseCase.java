package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionView;

/**
 * Caso de uso de la distribución patrimonial.
 */
public interface ConsultarDistribucionPatrimonioUseCase {

    DistribucionView distribucion(ConsultarPatrimonioQuery query);
}
