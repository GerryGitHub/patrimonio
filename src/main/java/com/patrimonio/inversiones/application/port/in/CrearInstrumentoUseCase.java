package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.CrearInstrumentoCommand;
import com.patrimonio.inversiones.application.view.InstrumentoView;

/**
 * Caso de uso: registrar un instrumento.
 */
public interface CrearInstrumentoUseCase {

    InstrumentoView execute(CrearInstrumentoCommand command);
}
