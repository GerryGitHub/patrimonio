package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.RegistrarMovimientoCommand;
import com.patrimonio.inversiones.application.view.MovimientoView;

/**
 * Caso de uso de alta de un movimiento de posición.
 */
public interface RegistrarMovimientoUseCase {

    MovimientoView execute(RegistrarMovimientoCommand command);
}
