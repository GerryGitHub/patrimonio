package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.ActualizarInstrumentoCommand;
import com.patrimonio.inversiones.application.view.InstrumentoView;

/**
 * Caso de uso: editar un instrumento.
 */
public interface ActualizarInstrumentoUseCase {

    InstrumentoView execute(ActualizarInstrumentoCommand command);
}
