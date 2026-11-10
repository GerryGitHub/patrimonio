package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.query.ConsultarInstrumentoQuery;
import com.patrimonio.inversiones.application.view.InstrumentoView;

/**
 * Caso de uso: consultar el detalle de un instrumento.
 */
public interface ConsultarInstrumentoUseCase {

    InstrumentoView execute(ConsultarInstrumentoQuery query);
}
