package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.ActualizarPosicionCommand;
import com.patrimonio.inversiones.application.view.PosicionView;

/**
 * Caso de uso: corregir una posición existente.
 */
public interface ActualizarPosicionUseCase {

    PosicionView execute(ActualizarPosicionCommand command);
}
