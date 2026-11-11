package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.ReconstruirPosicionCommand;
import com.patrimonio.inversiones.application.view.ReconstruccionView;

/**
 * Caso de uso de reconstrucción y verificación de una posición.
 */
public interface ReconstruirPosicionUseCase {

    ReconstruccionView execute(ReconstruirPosicionCommand command);
}
