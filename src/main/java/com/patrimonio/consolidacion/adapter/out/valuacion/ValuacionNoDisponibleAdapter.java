package com.patrimonio.consolidacion.adapter.out.valuacion;

import com.patrimonio.consolidacion.application.port.out.ValuacionPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador por defecto mientras no exista un módulo de cotizaciones.
 *
 * <p>Responde que no hay valuaciones y nunca inventa precios. Los indicadores
 * que dependen de valor de mercado se presentan como no disponibles.</p>
 */
@Component
public class ValuacionNoDisponibleAdapter implements ValuacionPort {

    @Override
    public boolean disponible() {
        return false;
    }

    @Override
    public Optional<BigDecimal> valorMercado(UUID instrumentoId, LocalDate fecha) {
        return Optional.empty();
    }
}
