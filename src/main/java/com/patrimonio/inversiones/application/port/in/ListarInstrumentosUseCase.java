package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ListarInstrumentosQuery;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import java.util.List;

/**
 * Caso de uso: listar el catálogo de instrumentos.
 */
public interface ListarInstrumentosUseCase {

    List<InstrumentoView> execute(ListarInstrumentosQuery query);
}
