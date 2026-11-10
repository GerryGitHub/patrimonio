package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.port.in.ListarInstrumentosUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.query.FiltroInstrumentos;
import com.patrimonio.inversiones.application.query.ListarInstrumentosQuery;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del listado del catálogo de instrumentos.
 */
@Service
public class ListarInstrumentosService implements ListarInstrumentosUseCase {

    private final InstrumentoRepository instrumentos;
    private final InversionesVistas vistas;

    public ListarInstrumentosService(
        InstrumentoRepository instrumentos,
        InversionesVistas vistas
    ) {
        this.instrumentos = instrumentos;
        this.vistas = vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentoView> execute(ListarInstrumentosQuery query) {
        Objects.requireNonNull(query, "query");

        FiltroInstrumentos filtro = new FiltroInstrumentos(
            TextosInversiones.opcional(query.busqueda()),
            query.claseActivoId(),
            query.tipoInstrumentoId(),
            query.activo());

        List<Instrumento> encontrados = instrumentos.listar(filtro);
        Map<UUID, InstrumentoView> porId = vistas.instrumentos(encontrados);
        return encontrados.stream()
            .map(instrumento -> porId.get(instrumento.id()))
            .toList();
    }
}
