package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.port.in.ListarTiposCuentaUseCase;
import com.patrimonio.cuentas.application.port.out.TipoCuentaRepository;
import com.patrimonio.cuentas.application.view.TipoCuentaView;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del listado del catálogo de tipos de cuenta.
 */
@Service
public class ListarTiposCuentaService implements ListarTiposCuentaUseCase {

    private final TipoCuentaRepository tiposCuenta;

    public ListarTiposCuentaService(TipoCuentaRepository tiposCuenta) {
        this.tiposCuenta = tiposCuenta;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipoCuentaView> execute() {
        return tiposCuenta.listarActivos().stream()
            .map(TipoCuentaView::from)
            .toList();
    }
}
