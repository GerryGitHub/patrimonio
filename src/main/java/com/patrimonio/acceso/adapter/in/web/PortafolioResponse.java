package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.UUID;

/**
 * Portafolio autorizado que se devuelve por HTTP.
 */
public record PortafolioResponse(
    UUID id,
    String nombre,
    UUID monedaBaseId
) {

    static PortafolioResponse from(PortafolioResumen portafolio) {
        return new PortafolioResponse(
            portafolio.id(),
            portafolio.nombre(),
            portafolio.monedaBaseId());
    }
}
