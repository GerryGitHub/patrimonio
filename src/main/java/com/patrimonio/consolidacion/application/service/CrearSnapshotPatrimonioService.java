package com.patrimonio.consolidacion.application.service;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.consolidacion.application.command.CrearSnapshotPatrimonioCommand;
import com.patrimonio.consolidacion.application.port.in.CrearSnapshotPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.out.SnapshotPatrimonioRepository;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.support.MovimientosPatrimonio;
import com.patrimonio.consolidacion.application.support.SnapshotPatrimonioViewAssembler;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import com.patrimonio.consolidacion.domain.error.SnapshotInvalidoException;
import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.cuentas.api.port.CuentaPatrimonioPort;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.inversiones.api.port.MovimientoPatrimonioPort;
import com.patrimonio.inversiones.api.port.PosicionPatrimonioPort;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de captura manual de snapshots patrimoniales.
 *
 * <p>El snapshot guarda solo datos reales: capital invertido a costo y
 * resultado realizado. El efectivo, los pasivos, el valor de mercado y el
 * patrimonio neto permanecen {@code NULL}. La cobertura identifica que se
 * trata de inversiones a costo y no de patrimonio neto completo.</p>
 */
@Service
public class CrearSnapshotPatrimonioService
    implements CrearSnapshotPatrimonioUseCase {

    private final AccesoPatrimonio acceso;
    private final PortafolioQueryPort portafolios;
    private final CuentaPatrimonioPort cuentas;
    private final PosicionPatrimonioPort posiciones;
    private final MovimientoPatrimonioPort movimientos;
    private final SnapshotPatrimonioRepository snapshots;
    private final MovimientosPatrimonio movimientosPatrimonio;
    private final SnapshotPatrimonioViewAssembler assembler;

    public CrearSnapshotPatrimonioService(
        AccesoPatrimonio acceso,
        PortafolioQueryPort portafolios,
        CuentaPatrimonioPort cuentas,
        PosicionPatrimonioPort posiciones,
        MovimientoPatrimonioPort movimientos,
        SnapshotPatrimonioRepository snapshots,
        MovimientosPatrimonio movimientosPatrimonio,
        SnapshotPatrimonioViewAssembler assembler
    ) {
        this.acceso = acceso;
        this.portafolios = portafolios;
        this.cuentas = cuentas;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.snapshots = snapshots;
        this.movimientosPatrimonio = movimientosPatrimonio;
        this.assembler = assembler;
    }

    @Override
    @Transactional
    public List<SnapshotView> execute(CrearSnapshotPatrimonioCommand command) {
        Objects.requireNonNull(command, "command");
        acceso.exigirEscritura(command.usuarioId(), command.portafolioId());
        PortafolioResumen portafolio = portafolios
            .buscarPorId(command.portafolioId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el portafolio indicado."));

        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        LocalDate fecha = command.fecha() == null ? hoy : command.fecha();
        if (!fecha.equals(hoy)) {
            throw new SnapshotInvalidoException(
                "El snapshot manual solo puede capturar la fecha actual; "
                    + "no se reconstruyen fechas pasadas con datos presentes.");
        }

        List<CuentaPatrimonio> cuentasPortafolio =
            cuentas.listarPorPortafolio(portafolio.id());
        Set<UUID> cuentaIds = cuentasPortafolio.stream()
            .map(CuentaPatrimonio::id)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PosicionPatrimonio> posicionesActivas = cuentaIds.isEmpty()
            ? List.of()
            : posiciones.listarPorCuentas(cuentaIds, true);
        List<MovimientoPatrimonio> historial =
            movimientos.listarPorPortafolio(portafolio.id(), null, hoy);

        Map<UUID, BigDecimal> capital = new LinkedHashMap<>();
        for (PosicionPatrimonio posicion : posicionesActivas) {
            capital.merge(
                posicion.monedaId(),
                posicion.costoTotal(),
                BigDecimal::add);
        }

        Map<UUID, BigDecimal> resultado = new LinkedHashMap<>();
        for (MovimientoPatrimonio movimiento
            : movimientosPatrimonio.efectivos(historial)) {
            BigDecimal valor = movimiento.resultadoRealizado() == null
                ? BigDecimal.ZERO
                : movimiento.resultadoRealizado();
            resultado.merge(movimiento.monedaId(), valor, BigDecimal::add);
        }

        Set<UUID> monedasConDatos = new LinkedHashSet<>();
        monedasConDatos.addAll(capital.keySet());
        monedasConDatos.addAll(resultado.keySet());
        if (monedasConDatos.isEmpty()) {
            monedasConDatos.add(portafolio.monedaBase().id());
        }

        List<SnapshotView> vistas = new ArrayList<>();
        for (UUID monedaId : monedasConDatos) {
            SnapshotPatrimonio snapshot = snapshots
                .buscarPorClave(
                    portafolio.id(),
                    fecha,
                    monedaId,
                    SnapshotPatrimonio.FUENTE_MANUAL)
                .orElseGet(() -> snapshots.guardar(
                    SnapshotPatrimonio.crear(
                        UUID.randomUUID(),
                        portafolio.id(),
                        fecha,
                        monedaId,
                        capital.getOrDefault(monedaId, BigDecimal.ZERO),
                        resultado.getOrDefault(monedaId, BigDecimal.ZERO),
                        SnapshotPatrimonio.COBERTURA_INVERSIONES,
                        SnapshotPatrimonio.LIMITACIONES_POR_DEFECTO,
                        Instant.now())));
            vistas.add(assembler.vista(snapshot));
        }
        return vistas;
    }
}
