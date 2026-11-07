package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.port.in.ListarInstitucionesUseCase;
import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.application.query.ListarInstitucionesQuery;
import com.patrimonio.cuentas.application.view.InstitucionView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del listado del catálogo de instituciones.
 */
@Service
public class ListarInstitucionesService implements ListarInstitucionesUseCase {

    private final InstitucionRepository instituciones;

    public ListarInstitucionesService(InstitucionRepository instituciones) {
        this.instituciones = instituciones;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstitucionView> execute(ListarInstitucionesQuery query) {
        Objects.requireNonNull(query, "query");
        return instituciones.listar(query.tipo(), query.activo()).stream()
            .map(InstitucionView::from)
            .toList();
    }
}
