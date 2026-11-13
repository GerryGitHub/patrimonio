package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarEvolucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraEvolucionPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.support.ContextoPatrimonial;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EvolucionView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de la evolución histórica patrimonial.
 */
@Service
public class ConsultarEvolucionPatrimonioService
    implements ConsultarEvolucionPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final CalculadoraEvolucionPatrimonio calculadora;

    public ConsultarEvolucionPatrimonioService(
        CargadorContextoPatrimonial cargador,
        CalculadoraEvolucionPatrimonio calculadora
    ) {
        this.cargador = cargador;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public EvolucionView execute(ConsultarPatrimonioQuery query) {
        ContextoPatrimonial contexto = cargador.cargar(query);
        return calculadora.calcular(contexto, query, contexto.hasta());
    }
}
