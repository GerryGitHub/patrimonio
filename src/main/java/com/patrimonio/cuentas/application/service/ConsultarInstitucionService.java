package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.port.in.ConsultarInstitucionUseCase;
import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.application.view.InstitucionView;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la consulta de una institución por identificador.
 */
@Service
public class ConsultarInstitucionService implements ConsultarInstitucionUseCase {

    private final InstitucionRepository instituciones;

    public ConsultarInstitucionService(InstitucionRepository instituciones) {
        this.instituciones = instituciones;
    }

    @Override
    @Transactional(readOnly = true)
    public InstitucionView execute(UUID institucionId) {
        if (institucionId == null) {
            throw new NotFoundException("No existe la institución indicada.");
        }
        return instituciones.buscarPorId(institucionId)
            .map(InstitucionView::from)
            .orElseThrow(() -> new NotFoundException(
                "No existe la institución indicada."));
    }
}
