package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.query.ConsultarCuentaQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;

/**
 * Caso de uso: consultar el detalle de una cuenta.
 */
public interface ConsultarCuentaUseCase {

    CuentaFinancieraView execute(ConsultarCuentaQuery query);
}
