package com.patrimonio.cuentas.adapter.out.persistence.adapter;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.adapter.out.persistence.mapper.CuentaFinancieraMapper;
import com.patrimonio.cuentas.adapter.out.persistence.repository.CuentaFinancieraJpaRepository;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.query.FiltroCuentas;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que implementa el puerto de cuentas usando Spring Data
 * JPA y el mapper explícito.
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
        return listarPorPortafolio(portafolioId, new FiltroCuentas(null, null, null));
    }

    @Override
    public List<CuentaFinanciera> listarPorPortafolio(
        UUID portafolioId,
        FiltroCuentas filtro
    ) {
        FiltroCuentas aplicado = filtro == null
            ? new FiltroCuentas(null, null, null)
            : filtro;
        return repository.listarPorPortafolio(
                portafolioId,
                aplicado.institucionId(),
                aplicado.tipoCuentaId(),
                aplicado.activo())
            .stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<CuentaFinanciera> buscarPorId(UUID cuentaId) {
        return repository.findById(cuentaId).map(mapper::toDomain);
    }

    @Override
    public boolean existeNumeroEnInstitucion(
        UUID institucionId,
        String numeroCuenta,
        UUID excluirCuentaId
    ) {
        return repository.existeNumeroEnInstitucion(
            institucionId,
            numeroCuenta,
            excluirCuentaId);
    }

    @Override
    public CuentaFinanciera guardar(CuentaFinanciera cuenta) {
        Optional<CuentaFinancieraEntity> existente =
            repository.findById(cuenta.id());
        if (existente.isPresent()) {
            CuentaFinancieraEntity entity = existente.get();
            mapper.actualizarEntidad(entity, cuenta);
            return mapper.toDomain(repository.save(entity));
        }
        return mapper.toDomain(repository.save(mapper.toEntity(cuenta)));
    }
}
