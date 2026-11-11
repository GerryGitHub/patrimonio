package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.command.RevertirMovimientoCommand;
import com.patrimonio.inversiones.application.view.MovimientoView;

/**
 * Caso de uso de reversión de un movimiento confirmado.
 */
public interface RevertirMovimientoUseCase {

    MovimientoView execute(RevertirMovimientoCommand command);
}
