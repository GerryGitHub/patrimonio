package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.LiquidezView;

/**
 * Caso de uso de liquidez y capital comprometido.
 */
public interface ConsultarLiquidezPatrimonioUseCase {

    LiquidezView execute(ConsultarPatrimonioQuery query);
}
