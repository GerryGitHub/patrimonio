package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.command.CrearInstitucionCommand;
import com.patrimonio.cuentas.application.view.InstitucionView;

/**
 * Caso de uso: registrar una institución financiera.
 */
public interface CrearInstitucionUseCase {

    InstitucionView execute(CrearInstitucionCommand command);
}
