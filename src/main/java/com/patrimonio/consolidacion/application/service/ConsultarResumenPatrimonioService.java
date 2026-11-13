package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarResumenPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraResumenPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ResumenView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso del resumen patrimonial.
 */
@Service
public class ConsultarResumenPatrimonioService
    implements ConsultarResumenPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final CalculadoraResumenPatrimonio calculadora;

    public ConsultarResumenPatrimonioService(
        CargadorContextoPatrimonial cargador,
        CalculadoraResumenPatrimonio calculadora
    ) {
        this.cargador = cargador;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenView execute(ConsultarPatrimonioQuery query) {
        return calculadora.calcular(cargador.cargar(query), query);
    }
}
