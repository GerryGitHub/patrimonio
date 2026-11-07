package com.patrimonio.cuentas.application.command;

import com.patrimonio.cuentas.domain.model.TipoInstitucion;

/**
 * Datos para registrar una institución financiera.
 */
public record CrearInstitucionCommand(
    String nombre,
    TipoInstitucion tipo,
    String pais,
    String sitioWeb
) {
}
