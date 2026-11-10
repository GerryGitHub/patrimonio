package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.CambiarEstadoPosicionCommand;
import com.patrimonio.inversiones.application.view.PosicionView;

/**
 * Caso de uso: activar o desactivar una posición.
 */
public interface CambiarEstadoPosicionUseCase {

    PosicionView execute(CambiarEstadoPosicionCommand command);
}
