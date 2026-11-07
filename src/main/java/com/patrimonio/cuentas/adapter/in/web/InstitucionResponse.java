package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import java.util.UUID;

/**
 * DTO de respuesta de una institución financiera.
 */
public record InstitucionResponse(
    UUID id,
    String nombre,
    TipoInstitucion tipo,
    String pais,
    String sitioWeb,
    boolean activo
) {
}
