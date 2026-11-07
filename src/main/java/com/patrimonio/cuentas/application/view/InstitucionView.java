package com.patrimonio.cuentas.application.view;

import com.patrimonio.cuentas.domain.model.Institucion;
import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import java.util.UUID;

/**
 * Vista de aplicación de una institución financiera.
 */
public record InstitucionView(
    UUID id,
    String nombre,
    TipoInstitucion tipo,
    String pais,
    String sitioWeb,
    boolean activo
) {

    public static InstitucionView from(Institucion institucion) {
        return new InstitucionView(
            institucion.id(),
            institucion.nombre(),
            institucion.tipo(),
            institucion.pais().orElse(null),
            institucion.sitioWeb().orElse(null),
            institucion.activo());
    }
}
