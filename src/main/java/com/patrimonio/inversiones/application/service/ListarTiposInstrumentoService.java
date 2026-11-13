package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.port.in.ListarTiposInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.view.TipoInstrumentoView;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta de tipos de instrumento.
 */
@Service
public class ListarTiposInstrumentoService
    implements ListarTiposInstrumentoUseCase {

    private final CatalogoInstrumentoRepository catalogo;

    public ListarTiposInstrumentoService(
        CatalogoInstrumentoRepository catalogo
    ) {
        this.catalogo = catalogo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoInstrumentoView> execute(UUID claseActivoId) {
        List<TipoInstrumento> tipos = claseActivoId == null
            ? catalogo.listarTiposActivos()
            : catalogo.listarTiposActivosPorClase(claseActivoId);

        Map<UUID, ClaseActivo> clases = catalogo.buscarClasesPorIds(
                tipos.stream()
                    .map(TipoInstrumento::claseActivoId)
                    .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(ClaseActivo::id, Function.identity()));

        return tipos.stream()
            .map(tipo -> TipoInstrumentoView.from(
                tipo,
                clases.containsKey(tipo.claseActivoId())
                    ? clases.get(tipo.claseActivoId()).nombre()
                    : null))
            .toList();
    }
}
