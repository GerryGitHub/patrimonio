package com.patrimonio.consolidacion.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento patrimonial publicado por un módulo de contratos o rendimientos.
 *
 * <p>Se deja preparado para vencimientos, pagos de intereses, renovaciones y
 * dividendos. En esta entrega no existe una fuente de eventos y el adaptador
 * por defecto devuelve una lista vacía.</p>
 */
public record EventoPatrimonio(
    UUID id,
    UUID portafolioId,
    String tipo,
    LocalDate fecha,
    String descripcion,
    BigDecimal importe,
    UUID monedaId,
    String origen,
    UUID posicionId,
    UUID contratoId
) {
}
