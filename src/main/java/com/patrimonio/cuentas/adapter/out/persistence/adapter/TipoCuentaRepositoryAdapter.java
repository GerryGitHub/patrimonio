package com.patrimonio.cuentas.adapter.out.persistence.adapter;

import com.patrimonio.cuentas.adapter.out.persistence.mapper.TipoCuentaMapper;
import com.patrimonio.cuentas.adapter.out.persistence.repository.TipoCuentaJpaRepository;
import com.patrimonio.cuentas.application.port.out.TipoCuentaRepository;
import com.patrimonio.cuentas.domain.model.TipoCuenta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida del catálogo de tipos de cuenta.
 */
@Component
public class TipoCuentaRepositoryAdapter implements TipoCuentaRepository {

    private final TipoCuentaJpaRepository repository;
    private final TipoCuentaMapper mapper;

    public TipoCuentaRepositoryAdapter(
        TipoCuentaJpaRepository repository,
        TipoCuentaMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<TipoCuenta> listarActivos() {
        return repository.findByActivoTrueOrderByNombreAsc().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<TipoCuenta> buscarActivoPorId(UUID tipoCuentaId) {
        return repository.findByIdAndActivoTrue(tipoCuentaId)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<TipoCuenta> buscarPorId(UUID tipoCuentaId) {
        return repository.findById(tipoCuentaId).map(mapper::toDomain);
    }
}
