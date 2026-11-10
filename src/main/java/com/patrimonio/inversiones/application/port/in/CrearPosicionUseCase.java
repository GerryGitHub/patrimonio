package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.CrearPosicionCommand;
import com.patrimonio.inversiones.application.view.PosicionView;

/**
 * Caso de uso: registrar una posición inicial.
 */
public interface CrearPosicionUseCase {

    PosicionView execute(CrearPosicionCommand command);
}
