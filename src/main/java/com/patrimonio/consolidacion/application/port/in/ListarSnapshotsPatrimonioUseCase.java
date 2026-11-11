package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.query.ConsultarSnapshotsQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import java.util.List;

/**
 * Caso de uso de consulta de snapshots patrimoniales.
 */
public interface ListarSnapshotsPatrimonioUseCase {

    List<SnapshotView> listar(ConsultarSnapshotsQuery query);
}
