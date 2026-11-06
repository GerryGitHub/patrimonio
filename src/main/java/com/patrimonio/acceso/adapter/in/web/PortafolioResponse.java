package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.UUID;

/**
 * Portafolio autorizado que se devuelve por HTTP.
 *
 * <p>Incluye la moneda base anidada para que el cliente no tenga que resolver
 * el catálogo por su cuenta. Se conserva {@code monedaBaseId} por
 * compatibilidad con los consumidores existentes.</p>
 */
public record PortafolioResponse(
    UUID id,
    String nombre,
    String descripcion,
    UUID monedaBaseId,
    MonedaResponse monedaBase,
    boolean activo
) {

    static PortafolioResponse from(PortafolioResumen portafolio) {
        return new PortafolioResponse(
            portafolio.id(),
            portafolio.nombre(),
            portafolio.descripcion(),
            portafolio.monedaBaseId(),
            MonedaResponse.from(portafolio.monedaBase()),
            portafolio.activo());
    }
}
