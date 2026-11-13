package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.view.PatrimonioViews.IndicadorView;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Construcción de indicadores patrimoniales con su estado de información.
 *
 * <p>Un indicador sin datos se representa como {@code NO_DISPONIBLE} con un
 * motivo; nunca se rellena con cero.</p>
 */
@Component
public class IndicadoresPatrimonio {

    public IndicadorView disponible(
        String codigo,
        String etiqueta,
        TotalesMoneda totales,
        String motivo
    ) {
        return new IndicadorView(
            codigo,
            etiqueta,
            EstadoInformacion.DISPONIBLE,
            totales.vacio() ? motivo : null,
            totales.importes());
    }

    public IndicadorView parcial(
        String codigo,
        String etiqueta,
        TotalesMoneda totales,
        String motivo
    ) {
        return new IndicadorView(
            codigo,
            etiqueta,
            EstadoInformacion.PARCIAL,
            motivo,
            totales.importes());
    }

    public IndicadorView noDisponible(
        String codigo,
        String etiqueta,
        String motivo
    ) {
        return new IndicadorView(
            codigo,
            etiqueta,
            EstadoInformacion.NO_DISPONIBLE,
            motivo,
            List.of());
    }
}
