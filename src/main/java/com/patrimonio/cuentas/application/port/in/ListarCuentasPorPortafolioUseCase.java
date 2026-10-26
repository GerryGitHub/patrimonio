package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.List;

/**
 * Caso de uso: listar las cuentas financieras de un portafolio.
 */
public interface ListarCuentasPorPortafolioUseCase {

    /**
     * Devuelve las cuentas del portafolio, o una lista vacía si existe y no
     * tiene cuentas.
     *
     * @throws com.patrimonio.shared.domain.error.NotFoundException si el
     *     portafolio no existe
     */
    List<CuentaFinancieraView> consultar(ListarCuentasPorPortafolioQuery query);
}
