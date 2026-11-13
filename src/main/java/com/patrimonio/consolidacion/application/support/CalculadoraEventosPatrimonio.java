package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.port.out.EventoPatrimonio;
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonioPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventoView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventosView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cálculo de los próximos eventos patrimoniales.
 *
 * <p>Mientras no exista una fuente de contratos, pagos ni dividendos, la
 * respuesta se marca como {@code NO_DISPONIBLE} y no se crean eventos
 * ficticios.</p>
 */
@Component
public class CalculadoraEventosPatrimonio {

    private final EventoPatrimonioPort eventosPort;
    private final MonedasPatrimonio monedas;

    public CalculadoraEventosPatrimonio(
        EventoPatrimonioPort eventosPort,
        MonedasPatrimonio monedas
    ) {
        this.eventosPort = eventosPort;
        this.monedas = monedas;
    }

    public EventosView calcular(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query,
        LocalDate hasta
    ) {
        if (!eventosPort.disponible()) {
            return new EventosView(
                EstadoInformacion.NO_DISPONIBLE,
                "No existen contratos de inversión, pagos programados ni "
                    + "dividendos registrados.",
                List.of());
        }

        LocalDate desde = query.desde();
        LocalDate hastaFinal = query.hasta() == null ? hasta : query.hasta();
        List<EventoView> vistas = new ArrayList<>();
        for (PortafolioPatrimonio dato : contexto.portafolios()) {
            for (EventoPatrimonio evento : eventosPort.listar(
                dato.portafolio().id(), desde, hastaFinal)) {
                MonedaView monedaEvento = evento.monedaId() == null
                    ? null
                    : monedas.deCuenta(evento.monedaId());
                if (monedaEvento != null
                    && !monedas.coincide(query, monedaEvento.codigo())) {
                    continue;
                }
                vistas.add(new EventoView(
                    evento.id(),
                    evento.tipo(),
                    evento.fecha(),
                    evento.descripcion(),
                    evento.importe(),
                    monedaEvento,
                    evento.origen(),
                    evento.posicionId(),
                    evento.contratoId()));
            }
        }
        vistas.sort(Comparator.comparing(EventoView::fecha));
        return new EventosView(
            EstadoInformacion.DISPONIBLE,
            vistas.isEmpty()
                ? "No hay eventos programados en el período."
                : null,
            vistas);
    }
}
