package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.view.TipoCuentaView;
import java.util.List;

/**
 * Caso de uso: listar los tipos de cuenta activos.
 */
public interface ListarTiposCuentaUseCase {

    List<TipoCuentaView> execute();
}
