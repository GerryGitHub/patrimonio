package com.patrimonio.consolidacion.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto futuro de valuación de mercado.
 *
 * <p>En esta entrega no existe una fuente de cotizaciones implementada. El
 * adaptador por defecto responde que no hay valuaciones, de modo que los
 * indicadores dependientes de valor de mercado se presentan como no
 * disponibles en lugar de inventar precios.</p>
 */
public interface ValuacionPort {

    /**
     * Indica si existe una fuente de valuación utilizable.
     */
    boolean disponible();

    /**
     * Valor de mercado del instrumento en la fecha indicada.
     */
    Optional<BigDecimal> valorMercado(UUID instrumentoId, LocalDate fecha);
}
