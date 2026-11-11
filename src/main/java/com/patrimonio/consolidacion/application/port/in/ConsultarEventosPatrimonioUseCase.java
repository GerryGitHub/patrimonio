package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventosView;

/**
 * Caso de uso de próximos eventos patrimoniales.
 */
public interface ConsultarEventosPatrimonioUseCase {

    EventosView eventos(ConsultarPatrimonioQuery query);
}
