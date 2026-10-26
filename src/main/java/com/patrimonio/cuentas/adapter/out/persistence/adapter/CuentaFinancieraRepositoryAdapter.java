package com.patrimonio.cuentas.adapter.out.persistence.adapter;

import com.patrimonio.cuentas.adapter.out.persistence.mapper.CuentaFinancieraMapper;
import com.patrimonio.cuentas.adapter.out.persistence.repository.CuentaFinancieraJpaRepository;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa el puerto de consulta de cuentas usando
 * Spring Data JPA y el mapper explícito.
 */
@Component
public class CuentaFinancieraRepositoryAdapter implements CuentaFinancieraRepository {

    private final CuentaFinancieraJpaRepository repository;
    private final CuentaFinancieraMapper mapper;

    public CuentaFinancieraRepositoryAdapter(
        CuentaFinancieraJpaRepository repository,
        CuentaFinancieraMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CuentaFinanciera> listarPorPortafolio(UUID portafolioId) {
        return repository.listarPorPortafolio(portafolioId).stream()
            .map(mapper::toDomain)
            .toList();
    }
}
