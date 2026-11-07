package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.command.CambiarEstadoCuentaCommand;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;

/**
 * Caso de uso: cambiar el estado de una cuenta financiera.
 */
public interface CambiarEstadoCuentaUseCase {

    CuentaFinancieraView execute(CambiarEstadoCuentaCommand command);
}
