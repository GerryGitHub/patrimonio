package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.application.view.InstitucionView;
import org.springframework.stereotype.Component;

/**
 * Convierte vistas de instituciones en DTOs de respuesta.
 */
@Component
public class InstitucionResponseAssembler {

    public InstitucionResponse toResponse(InstitucionView institucion) {
        return new InstitucionResponse(
            institucion.id(),
            institucion.nombre(),
            institucion.tipo(),
            institucion.pais(),
            institucion.sitioWeb(),
            institucion.activo());
    }
}
