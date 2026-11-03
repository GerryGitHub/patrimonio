package com.patrimonio.acceso.application.service;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.acceso.application.port.in.ListarPortafoliosAutorizadosUseCase;
import com.patrimonio.acceso.application.query.ListarPortafoliosAutorizadosQuery;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso que lista los portafolios autorizados.
 *
 * <p>La consulta ya filtra por membresía, portafolio activo y usuario activo;
 * este servicio no amplía el resultado.</p>
 */
@Service
public class ListarPortafoliosAutorizadosService
    implements ListarPortafoliosAutorizadosUseCase {

    private final AccesoQueryPort accesoQueryPort;

    public ListarPortafoliosAutorizadosService(AccesoQueryPort accesoQueryPort) {
        this.accesoQueryPort = accesoQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortafolioResumen> execute(ListarPortafoliosAutorizadosQuery query) {
        Objects.requireNonNull(query, "query");
        return accesoQueryPort.listarAutorizados(query.usuarioId());
    }
}
