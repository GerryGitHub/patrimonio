package com.patrimonio.consolidacion.application.support;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Construcción de la vista de un snapshot patrimonial.
 *
 * <p>Centraliza el mapeo y la resolución de la moneda para que la creación y
 * el listado no mantengan dos versiones del mismo contrato.</p>
 */
@Component
public class SnapshotPatrimonioViewAssembler {

    private final MonedaCatalogoPort monedas;

    public SnapshotPatrimonioViewAssembler(MonedaCatalogoPort monedas) {
        this.monedas = monedas;
    }

    public SnapshotView vista(SnapshotPatrimonio snapshot) {
        return new SnapshotView(
            snapshot.id(),
            snapshot.portafolioId(),
            snapshot.fecha(),
            moneda(snapshot.monedaId()),
            snapshot.capitalInvertido(),
            snapshot.resultadoRealizado(),
            snapshot.efectivo(),
            snapshot.pasivos(),
            snapshot.valorMercado(),
            snapshot.patrimonioNeto(),
            snapshot.cobertura(),
            snapshot.limitaciones(),
            snapshot.fuente(),
            snapshot.fechaCreacion());
    }

    private MonedaView moneda(UUID monedaId) {
        MonedaResumen moneda = monedas.buscarPorId(monedaId)
            .orElseThrow(() -> new DatosPersistidosInvalidos(
                "El snapshot referencia una moneda inexistente: " + monedaId));
        return new MonedaView(
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales());
    }
}
