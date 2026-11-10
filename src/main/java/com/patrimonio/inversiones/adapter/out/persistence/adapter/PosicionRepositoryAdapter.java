package com.patrimonio.inversiones.adapter.out.persistence.adapter;

import com.patrimonio.inversiones.adapter.out.persistence.entity.PosicionEntity;
import com.patrimonio.inversiones.adapter.out.persistence.mapper.PosicionMapper;
import com.patrimonio.inversiones.adapter.out.persistence.repository.PosicionJpaRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.domain.model.Posicion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida de posiciones.
 */
@Component
public class PosicionRepositoryAdapter implements PosicionRepository {

    private final PosicionJpaRepository posiciones;
    private final PosicionMapper mapper;

    public PosicionRepositoryAdapter(
        PosicionJpaRepository posiciones,
        PosicionMapper mapper
    ) {
        this.posiciones = posiciones;
        this.mapper = mapper;
    }

    @Override
    public List<Posicion> listarPorCuentas(
        Collection<UUID> cuentaIds,
        UUID cuentaId,
        UUID instrumentoId,
        Collection<UUID> instrumentoIdsRestringidos,
        Boolean activo
    ) {
        if (cuentaIds.isEmpty()) {
            return List.of();
        }
        List<PosicionEntity> entidades =
            instrumentoIdsRestringidos == null
                ? posiciones.listarPorCuentas(
                    cuentaIds, cuentaId, instrumentoId, activo)
                : posiciones.listarPorCuentasEInstrumentos(
                    cuentaIds,
                    instrumentoIdsRestringidos,
                    cuentaId,
                    instrumentoId,
                    activo);
        return entidades.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Posicion> buscarPorId(UUID posicionId) {
        return posiciones.findById(posicionId).map(mapper::toDomain);
    }

    @Override
    public Posicion guardar(Posicion posicion) {
        Optional<PosicionEntity> existente = posiciones.findById(posicion.id());
        if (existente.isPresent()) {
            PosicionEntity entidad = existente.get();
            mapper.actualizarEntidad(entidad, posicion);
            return mapper.toDomain(posiciones.save(entidad));
        }
        return mapper.toDomain(posiciones.save(mapper.toEntity(posicion)));
    }
}
