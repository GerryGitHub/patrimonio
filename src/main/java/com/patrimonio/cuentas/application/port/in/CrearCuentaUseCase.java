package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.command.CrearCuentaCommand;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;

/**
 * Caso de uso: crear una cuenta financiera.
 */
public interface CrearCuentaUseCase {

    CuentaFinancieraView execute(CrearCuentaCommand command);
}
