package com.patrimonio.consolidacion.adapter.out.persistence.repository;

import com.patrimonio.consolidacion.adapter.out.persistence.entity.SnapshotPatrimonioEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data de snapshots patrimoniales.
 *
 * <p>La clave natural portafolio, fecha, moneda y fuente es única en la base,
 * lo que hace idempotente la captura manual.</p>
 */
public interface SnapshotPatrimonioJpaRepository
    extends JpaRepository<SnapshotPatrimonioEntity, UUID> {

    Optional<SnapshotPatrimonioEntity>
        findByPortafolioIdAndFechaAndMonedaIdAndFuente(
            UUID portafolioId,
            LocalDate fecha,
            UUID monedaId,
            String fuente);

    List<SnapshotPatrimonioEntity>
        findByPortafolioIdOrderByFechaDescFechaCreacionDesc(UUID portafolioId);
}
