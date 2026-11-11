package com.patrimonio.consolidacion.application.port.out;

import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de snapshots patrimoniales.
 */
public interface SnapshotPatrimonioRepository {

    SnapshotPatrimonio guardar(SnapshotPatrimonio snapshot);

    Optional<SnapshotPatrimonio> buscarPorClave(
        UUID portafolioId,
        LocalDate fecha,
        UUID monedaId,
        String fuente);

    List<SnapshotPatrimonio> listarPorPortafolio(UUID portafolioId);
}
