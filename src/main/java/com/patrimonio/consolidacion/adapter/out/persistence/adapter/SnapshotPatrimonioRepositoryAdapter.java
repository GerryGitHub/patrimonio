package com.patrimonio.consolidacion.adapter.out.persistence.adapter;

import com.patrimonio.consolidacion.adapter.out.persistence.entity.SnapshotPatrimonioEntity;
import com.patrimonio.consolidacion.adapter.out.persistence.mapper.SnapshotPatrimonioMapper;
import com.patrimonio.consolidacion.adapter.out.persistence.repository.SnapshotPatrimonioJpaRepository;
import com.patrimonio.consolidacion.application.port.out.SnapshotPatrimonioRepository;
import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistencia de snapshots patrimoniales.
 */
@Component
public class SnapshotPatrimonioRepositoryAdapter
    implements SnapshotPatrimonioRepository {

    private final SnapshotPatrimonioJpaRepository snapshots;
    private final SnapshotPatrimonioMapper mapper;

    public SnapshotPatrimonioRepositoryAdapter(
        SnapshotPatrimonioJpaRepository snapshots,
        SnapshotPatrimonioMapper mapper
    ) {
        this.snapshots = snapshots;
        this.mapper = mapper;
    }

    @Override
    public SnapshotPatrimonio guardar(SnapshotPatrimonio snapshot) {
        SnapshotPatrimonioEntity guardado =
            snapshots.save(mapper.toEntity(snapshot));
        return mapper.toDomain(guardado);
    }

    @Override
    public Optional<SnapshotPatrimonio> buscarPorClave(
        UUID portafolioId,
        LocalDate fecha,
        UUID monedaId,
        String fuente
    ) {
        return snapshots
            .findByPortafolioIdAndFechaAndMonedaIdAndFuente(
                portafolioId, fecha, monedaId, fuente)
            .map(mapper::toDomain);
    }

    @Override
    public List<SnapshotPatrimonio> listarPorPortafolio(UUID portafolioId) {
        return snapshots
            .findByPortafolioIdOrderByFechaDescFechaCreacionDesc(portafolioId)
            .stream()
            .map(mapper::toDomain)
            .toList();
    }
}
