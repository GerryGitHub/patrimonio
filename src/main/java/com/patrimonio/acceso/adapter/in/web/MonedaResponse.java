package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import java.util.UUID;

/**
 * Moneda del catálogo que se devuelve por HTTP.
 */
public record MonedaResponse(
    UUID id,
    String codigo,
    String nombre,
    String simbolo,
    int decimales
) {

    static MonedaResponse from(MonedaResumen moneda) {
        return new MonedaResponse(
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales());
    }
}
