package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.repository.PortafolioJpaRepository;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa el puerto de consulta de portafolios.
 *
 * <p>Es la única vía por la que {@code cuentas} puede comprobar la existencia
 * de un portafolio: no accede directamente a la tabla {@code portafolio}.</p>
 */
@Component
public class PortafolioQueryAdapter implements PortafolioQueryPort {

    private final PortafolioJpaRepository repository;

    public PortafolioQueryAdapter(PortafolioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PortafolioResumen> buscarPorId(UUID portafolioId) {
        return repository.buscarResumenPorId(portafolioId);
    }
}
