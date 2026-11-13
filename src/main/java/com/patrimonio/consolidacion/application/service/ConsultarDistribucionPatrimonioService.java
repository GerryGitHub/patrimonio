package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarDistribucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraDistribucionPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de la distribución patrimonial.
 */
@Service
public class ConsultarDistribucionPatrimonioService
    implements ConsultarDistribucionPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final CalculadoraDistribucionPatrimonio calculadora;

    public ConsultarDistribucionPatrimonioService(
        CargadorContextoPatrimonial cargador,
        CalculadoraDistribucionPatrimonio calculadora
    ) {
        this.cargador = cargador;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public DistribucionView execute(ConsultarPatrimonioQuery query) {
        return calculadora.calcular(cargador.cargar(query), query);
    }
}
