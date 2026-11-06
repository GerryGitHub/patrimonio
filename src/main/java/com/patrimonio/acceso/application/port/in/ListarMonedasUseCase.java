package com.patrimonio.acceso.application.port.in;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import java.util.List;

/**
 * Caso de uso de consulta del catálogo de monedas activas.
 */
public interface ListarMonedasUseCase {

    List<MonedaResumen> execute();
}
