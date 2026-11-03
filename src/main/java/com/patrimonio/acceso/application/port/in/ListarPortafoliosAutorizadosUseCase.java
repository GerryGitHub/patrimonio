package com.patrimonio.acceso.application.port.in;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.application.query.ListarPortafoliosAutorizadosQuery;
import java.util.List;

/**
 * Caso de uso: listar los portafolios autorizados de un usuario.
 */
public interface ListarPortafoliosAutorizadosUseCase {

    List<PortafolioResumen> execute(ListarPortafoliosAutorizadosQuery query);
}
