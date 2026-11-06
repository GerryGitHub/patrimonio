package com.patrimonio.acceso.application.service;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.application.port.in.ListarMonedasUseCase;
import com.patrimonio.acceso.application.port.out.MonedaQueryPort;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso que lista el catálogo de monedas activas.
 */
@Service
public class ListarMonedasService implements ListarMonedasUseCase {

    private final MonedaQueryPort monedas;

    public ListarMonedasService(MonedaQueryPort monedas) {
        this.monedas = monedas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonedaResumen> execute() {
        return monedas.listarActivas();
    }
}
