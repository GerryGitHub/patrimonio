package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.view.ClaseActivoView;
import com.patrimonio.inversiones.application.view.TipoInstrumentoView;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar las clases y tipos de instrumento para filtros y
 * formularios.
 */
public interface ListarCatalogoInstrumentosUseCase {

    List<ClaseActivoView> listarClases();

    List<TipoInstrumentoView> listarTipos(UUID claseActivoId);
}
