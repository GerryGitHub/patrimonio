package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.CambiarEstadoInstrumentoCommand;
import com.patrimonio.inversiones.application.view.InstrumentoView;

/**
 * Caso de uso: activar o desactivar un instrumento.
 */
public interface CambiarEstadoInstrumentoUseCase {

    InstrumentoView execute(CambiarEstadoInstrumentoCommand command);
}
