package com.patrimonio.consolidacion.adapter.out.evento;

import com.patrimonio.consolidacion.application.port.out.EventoPatrimonio;
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonioPort;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador por defecto mientras no exista el módulo de contratos y pagos.
 *
 * <p>No crea vencimientos, dividendos ni pagos ficticios: informa que la
 * fuente no está disponible y devuelve una lista vacía.</p>
 */
@Component
public class EventoNoDisponibleAdapter implements EventoPatrimonioPort {

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public List<EventoPatrimonio> listar(
        UUID portafolioId,
        LocalDate desde,
        LocalDate hasta
    ) {
        return List.of();
    }
}
