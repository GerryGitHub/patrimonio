package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarEventosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraEventosPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.support.ContextoPatrimonial;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventosView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de próximos eventos patrimoniales.
 */
@Service
public class ConsultarEventosPatrimonioService
    implements ConsultarEventosPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final CalculadoraEventosPatrimonio calculadora;

    public ConsultarEventosPatrimonioService(
        CargadorContextoPatrimonial cargador,
        CalculadoraEventosPatrimonio calculadora
    ) {
        this.cargador = cargador;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public EventosView execute(ConsultarPatrimonioQuery query) {
        ContextoPatrimonial contexto = cargador.cargar(query);
        return calculadora.calcular(contexto, query, contexto.hasta());
    }
}
