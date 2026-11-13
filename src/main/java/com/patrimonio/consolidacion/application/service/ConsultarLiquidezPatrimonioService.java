package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarLiquidezPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraLiquidezPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.LiquidezView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de liquidez y capital comprometido.
 */
@Service
public class ConsultarLiquidezPatrimonioService
    implements ConsultarLiquidezPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final CalculadoraLiquidezPatrimonio calculadora;

    public ConsultarLiquidezPatrimonioService(
        CargadorContextoPatrimonial cargador,
        CalculadoraLiquidezPatrimonio calculadora
    ) {
        this.cargador = cargador;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidezView execute(ConsultarPatrimonioQuery query) {
        return calculadora.calcular(cargador.cargar(query), query);
    }
}
