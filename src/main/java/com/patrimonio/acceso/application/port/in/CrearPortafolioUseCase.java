package com.patrimonio.acceso.application.port.in;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.application.command.CrearPortafolioCommand;

/**
 * Caso de uso de creación de portafolios.
 */
public interface CrearPortafolioUseCase {

    PortafolioResumen execute(CrearPortafolioCommand command);
}
