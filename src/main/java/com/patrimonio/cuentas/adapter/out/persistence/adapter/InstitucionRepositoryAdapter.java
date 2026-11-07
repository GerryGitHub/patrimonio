package com.patrimonio.cuentas.adapter.out.persistence.adapter;

import com.patrimonio.cuentas.adapter.out.persistence.mapper.InstitucionMapper;
import com.patrimonio.cuentas.adapter.out.persistence.repository.InstitucionJpaRepository;
import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.domain.model.Institucion;
import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida del catálogo de instituciones.
 */
@Component
public class InstitucionRepositoryAdapter implements InstitucionRepository {

    private final InstitucionJpaRepository repository;
    private final InstitucionMapper mapper;

    public InstitucionRepositoryAdapter(
        InstitucionJpaRepository repository,
        InstitucionMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Institucion> listar(TipoInstitucion tipo, Boolean activo) {
        String tipoAlmacenado = tipo == null ? null : tipo.name();
        return repository.listar(tipoAlmacenado, activo).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Institucion> buscarPorId(UUID institucionId) {
        return repository.findById(institucionId).map(mapper::toDomain);
    }

    @Override
    public boolean existePorNombre(String nombre) {
        return repository.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public Institucion guardar(Institucion institucion) {
        return mapper.toDomain(repository.save(mapper.toEntity(institucion)));
    }
}
