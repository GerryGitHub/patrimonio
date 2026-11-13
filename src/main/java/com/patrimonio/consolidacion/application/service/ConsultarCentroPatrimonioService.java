package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ConsultarCentroPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.CalculadoraDistribucionPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraEventosPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraEvolucionPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraLiquidezPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraRendimientosPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraResumenPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.support.ContextoPatrimonial;
import com.patrimonio.consolidacion.application.support.MetadatosPatrimonio;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CentroView;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso del Centro de Patrimonio consolidado.
 *
 * <p>Carga el contexto una sola vez y compone los resultados de las consultas
 * individuales. No mantiene una segunda versión de las fórmulas: cada bloque
 * se delega en su calculadora.</p>
 */
@Service
public class ConsultarCentroPatrimonioService
    implements ConsultarCentroPatrimonioUseCase {

    private final CargadorContextoPatrimonial cargador;
    private final MetadatosPatrimonio metadatos;
    private final CalculadoraResumenPatrimonio resumen;
    private final CalculadoraDistribucionPatrimonio distribucion;
    private final CalculadoraEvolucionPatrimonio evolucion;
    private final CalculadoraRendimientosPatrimonio rendimientos;
    private final CalculadoraLiquidezPatrimonio liquidez;
    private final CalculadoraEventosPatrimonio eventos;

    public ConsultarCentroPatrimonioService(
        CargadorContextoPatrimonial cargador,
        MetadatosPatrimonio metadatos,
        CalculadoraResumenPatrimonio resumen,
        CalculadoraDistribucionPatrimonio distribucion,
        CalculadoraEvolucionPatrimonio evolucion,
        CalculadoraRendimientosPatrimonio rendimientos,
        CalculadoraLiquidezPatrimonio liquidez,
        CalculadoraEventosPatrimonio eventos
    ) {
        this.cargador = cargador;
        this.metadatos = metadatos;
        this.resumen = resumen;
        this.distribucion = distribucion;
        this.evolucion = evolucion;
        this.rendimientos = rendimientos;
        this.liquidez = liquidez;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public CentroView execute(ConsultarPatrimonioQuery query) {
        ContextoPatrimonial contexto = cargador.cargar(query);
        LocalDate hasta = contexto.hasta();
        return new CentroView(
            metadatos.meta(contexto, query),
            resumen.calcular(contexto, query),
            distribucion.calcular(contexto, query),
            evolucion.calcular(contexto, query, hasta),
            rendimientos.calcular(contexto, query, hasta),
            liquidez.calcular(contexto, query),
            eventos.calcular(contexto, query, hasta));
    }
}
