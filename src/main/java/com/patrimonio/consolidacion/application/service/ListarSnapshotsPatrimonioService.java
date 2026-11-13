package com.patrimonio.consolidacion.application.service;

import com.patrimonio.consolidacion.application.port.in.ListarSnapshotsPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.out.SnapshotPatrimonioRepository;
import com.patrimonio.consolidacion.application.query.ConsultarSnapshotsQuery;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.support.SnapshotPatrimonioViewAssembler;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de consulta de snapshots patrimoniales.
 */
@Service
public class ListarSnapshotsPatrimonioService
    implements ListarSnapshotsPatrimonioUseCase {

    private final AccesoPatrimonio acceso;
    private final SnapshotPatrimonioRepository snapshots;
    private final SnapshotPatrimonioViewAssembler assembler;

    public ListarSnapshotsPatrimonioService(
        AccesoPatrimonio acceso,
        SnapshotPatrimonioRepository snapshots,
        SnapshotPatrimonioViewAssembler assembler
    ) {
        this.acceso = acceso;
        this.snapshots = snapshots;
        this.assembler = assembler;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnapshotView> execute(ConsultarSnapshotsQuery query) {
        Objects.requireNonNull(query, "query");
        acceso.exigirLectura(query.usuarioId(), query.portafolioId());
        return snapshots.listarPorPortafolio(query.portafolioId()).stream()
            .map(assembler::vista)
            .toList();
    }
}
