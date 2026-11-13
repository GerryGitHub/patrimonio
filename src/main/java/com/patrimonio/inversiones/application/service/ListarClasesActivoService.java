package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.port.in.ListarClasesActivoUseCase;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.view.ClaseActivoView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta de clases de activo.
 */
@Service
public class ListarClasesActivoService implements ListarClasesActivoUseCase {

    private final CatalogoInstrumentoRepository catalogo;

    public ListarClasesActivoService(
        CatalogoInstrumentoRepository catalogo
    ) {
        this.catalogo = catalogo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClaseActivoView> execute() {
        return catalogo.listarClasesActivas().stream()
            .map(ClaseActivoView::from)
            .toList();
    }
}
