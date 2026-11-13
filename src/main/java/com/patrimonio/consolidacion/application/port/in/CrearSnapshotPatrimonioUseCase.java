package com.patrimonio.consolidacion.application.port.in;

import com.patrimonio.consolidacion.application.command.CrearSnapshotPatrimonioCommand;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import java.util.List;

/**
 * Caso de uso de captura manual de snapshots patrimoniales.
 */
public interface CrearSnapshotPatrimonioUseCase {

    List<SnapshotView> execute(CrearSnapshotPatrimonioCommand command);
}
