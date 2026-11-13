package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.view.TipoInstrumentoView;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar los tipos de instrumento activos.
 */
public interface ListarTiposInstrumentoUseCase {

    List<TipoInstrumentoView> execute(UUID claseActivoId);
}
