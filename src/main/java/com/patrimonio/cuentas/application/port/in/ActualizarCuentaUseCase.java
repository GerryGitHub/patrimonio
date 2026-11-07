package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.command.ActualizarCuentaCommand;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;

/**
 * Caso de uso: editar una cuenta financiera existente.
 */
public interface ActualizarCuentaUseCase {

    CuentaFinancieraView execute(ActualizarCuentaCommand command);
}
