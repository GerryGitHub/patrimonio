package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de alta de instituciones financieras.
 */
public record CrearInstitucionRequest(
    @NotBlank @Size(max = 150) String nombre,
    @NotNull TipoInstitucion tipo,
    @Size(max = 100) String pais,
    @Size(max = 255) String sitioWeb
) {
}
