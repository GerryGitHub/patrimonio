package com.patrimonio.consolidacion.application.service;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.consolidacion.application.command.CrearSnapshotPatrimonioCommand;
import com.patrimonio.consolidacion.application.port.in.CrearSnapshotPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ListarSnapshotsPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.out.SnapshotPatrimonioRepository;
import com.patrimonio.consolidacion.application.query.ConsultarSnapshotsQuery;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import com.patrimonio.consolidacion.domain.error.SnapshotInvalidoException;
import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.cuentas.api.port.CuentaPatrimonioPort;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.inversiones.api.port.MovimientoPatrimonioPort;
import com.patrimonio.inversiones.api.port.PosicionPatrimonioPort;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
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
 * Captura y consulta de snapshots patrimoniales manuales.
 *
 * <p>El snapshot guarda solo datos reales: capital invertido a costo y
 * resultado realizado. El efectivo, los pasivos, el valor de mercado y el
 * patrimonio neto permanecen {@code NULL}. La cobertura identifica que se
 * trata de inversiones a costo y no de patrimonio neto completo.</p>
 */
@Service
public class SnapshotPatrimonioService implements
    CrearSnapshotPatrimonioUseCase,
    ListarSnapshotsPatrimonioUseCase {

    private final AccesoPatrimonio acceso;
    private final PortafolioQueryPort portafolios;
    private final CuentaPatrimonioPort cuentas;
    private final PosicionPatrimonioPort posiciones;
    private final MovimientoPatrimonioPort movimientos;
    private final MonedaCatalogoPort monedas;
    private final SnapshotPatrimonioRepository snapshots;

    public SnapshotPatrimonioService(
        AccesoPatrimonio acceso,
        PortafolioQueryPort portafolios,
        CuentaPatrimonioPort cuentas,
        PosicionPatrimonioPort posiciones,
        MovimientoPatrimonioPort movimientos,
        MonedaCatalogoPort monedas,
        SnapshotPatrimonioRepository snapshots
    ) {
        this.acceso = acceso;
        this.portafolios = portafolios;
        this.cuentas = cuentas;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.monedas = monedas;
        this.snapshots = snapshots;
    }

    @Override
    @Transactional
    public List<SnapshotView> crear(CrearSnapshotPatrimonioCommand command) {
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

        Set<UUID> revertidos = historial.stream()
            .map(MovimientoPatrimonio::movimientoOrigenId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<UUID, BigDecimal> resultado = new LinkedHashMap<>();
        for (MovimientoPatrimonio movimiento : historial) {
            if (movimiento.reversion() || revertidos.contains(movimiento.id())) {
                continue;
            }
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
            vistas.add(vista(snapshot));
        }
        return vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SnapshotView> listar(ConsultarSnapshotsQuery query) {
        Objects.requireNonNull(query, "query");
        acceso.exigirLectura(query.usuarioId(), query.portafolioId());
        return snapshots.listarPorPortafolio(query.portafolioId()).stream()
            .map(this::vista)
            .toList();
    }

    private SnapshotView vista(SnapshotPatrimonio snapshot) {
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
