package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.view.InstitucionView;
import java.util.UUID;

/**
 * Caso de uso: consultar una institución por identificador.
 */
public interface ConsultarInstitucionUseCase {

    InstitucionView execute(UUID institucionId);
}
