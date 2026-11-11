package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.port.in.ListarMovimientosUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.query.ListarMovimientosQuery;
import com.patrimonio.inversiones.application.query.PaginaMovimientos;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.MovimientoView;
import com.patrimonio.inversiones.application.view.PaginaMovimientosView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devuelve el historial paginado de una posición.
 *
 * <p>El orden es cronológico ascendente por defecto; la API puede pedirlo
 * descendente para mostrar primero lo más reciente. Los filtros por tipo y
 * rango de fechas se aplican en la consulta.</p>
 */
@Service
public class ListarMovimientosService implements ListarMovimientosUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final MovimientoRepository movimientos;
    private final InversionesVistas vistas;

    public ListarMovimientosService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        PosicionRepository posiciones,
        MovimientoRepository movimientos,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.vistas = vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaMovimientosView execute(ListarMovimientosQuery query) {
        Objects.requireNonNull(query, "query");

        accesoPortafolio.exigirLectura(
            query.usuarioId(), query.portafolioId());

        Posicion posicion = posiciones.buscarPorId(query.posicionId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(query.portafolioId(), posicion.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        Instrumento instrumento = instrumentos
            .buscarPorId(posicion.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento de la posición."));

        PaginaMovimientos pagina = movimientos.listarPorPosicion(
            posicion.id(),
            query.filtro(),
            query.pagina(),
            query.tamano(),
            query.descendente());

        Set<UUID> revertidos = movimientos.buscarOrigenesRevertidos(
            pagina.items().stream().map(Movimiento::id).toList());

        Map<UUID, CuentaResumen> cuentasPorId = Map.of(cuenta.id(), cuenta);
        List<MovimientoView> items = vistas.movimientos(
            pagina.items(),
            List.of(instrumento),
            cuentasPorId,
            revertidos);

        int totalPaginas = pagina.total() == 0
            ? 0
            : (int) ((pagina.total() + query.tamano() - 1) / query.tamano());

        return new PaginaMovimientosView(
            items,
            query.pagina(),
            query.tamano(),
            pagina.total(),
            totalPaginas);
    }
}
