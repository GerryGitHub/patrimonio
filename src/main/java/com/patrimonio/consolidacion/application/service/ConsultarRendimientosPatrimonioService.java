package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarRendimientosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraRendimientosPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.support.ContextoPatrimonial;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.RendimientosView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de rendimientos y costos de operación.
 */
@Service
public class ConsultarRendimientosPatrimonioService
    implements ConsultarRendimientosPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final CalculadoraRendimientosPatrimonio calculadora;

    public ConsultarRendimientosPatrimonioService(
        CargadorContextoPatrimonial cargador,
        CalculadoraRendimientosPatrimonio calculadora
    ) {
        this.cargador = cargador;
        this.calculadora = calculadora;
    }

    @Override
    @Transactional(readOnly = true)
    public RendimientosView execute(ConsultarPatrimonioQuery query) {
        ContextoPatrimonial contexto = cargador.cargar(query);
        return calculadora.calcular(contexto, query, contexto.hasta());
    }
}
