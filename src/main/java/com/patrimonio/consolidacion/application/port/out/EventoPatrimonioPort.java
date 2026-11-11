package com.patrimonio.consolidacion.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Puerto futuro de eventos patrimoniales programados.
 *
 * <p>No se implementa todavía el módulo de contratos, pagos ni dividendos. El
 * adaptador por defecto indica que la fuente no está disponible y devuelve una
 * lista vacía, sin crear eventos ficticios.</p>
 */
public interface EventoPatrimonioPort {

    boolean disponible();

    List<EventoPatrimonio> listar(
        UUID portafolioId,
        LocalDate desde,
        LocalDate hasta);
}
