package com.patrimonio.inversiones.adapter.out.persistence.adapter;

import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.port.MovimientoPatrimonioPort;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adaptador del contrato público de movimientos patrimoniales.
 *
 * <p>Devuelve los efectos almacenados por el motor de movimientos sin
 * recalcularlos. Las reversiones llegan con el signo contrario al movimiento
 * original para que el consumidor pueda anular la operación sin doble conteo.</p>
 */
@Component
public class MovimientoPatrimonioAdapter implements MovimientoPatrimonioPort {

    private final MovimientoRepository movimientos;
    private final InstrumentoRepository instrumentos;
    private final InversionesVistas vistas;

    public MovimientoPatrimonioAdapter(
        MovimientoRepository movimientos,
        InstrumentoRepository instrumentos,
        InversionesVistas vistas
    ) {
        this.movimientos = movimientos;
        this.instrumentos = instrumentos;
        this.vistas = vistas;
    }

    @Override
    public List<MovimientoPatrimonio> listarPorPortafolio(
        UUID portafolioId,
        LocalDate desde,
        LocalDate hasta
    ) {
        List<Movimiento> encontrados =
            movimientos.listarPorPortafolio(portafolioId, desde, hasta);
        if (encontrados.isEmpty()) {
            return List.of();
        }

        Set<UUID> instrumentoIds = encontrados.stream()
            .map(Movimiento::instrumentoId)
            .collect(Collectors.toSet());
        List<Instrumento> referenciados =
            instrumentos.buscarPorIds(instrumentoIds);
        Map<UUID, InstrumentoView> vistasInstrumento =
            vistas.instrumentos(referenciados);

        return encontrados.stream()
            .map(movimiento -> convertir(movimiento, vistasInstrumento))
            .toList();
    }

    private MovimientoPatrimonio convertir(
        Movimiento movimiento,
        Map<UUID, InstrumentoView> vistasInstrumento
    ) {
        InstrumentoView instrumento = vistasInstrumento.get(
            movimiento.instrumentoId());
        if (instrumento == null) {
            throw new DatosPersistidosInvalidos(
                "El movimiento " + movimiento.id()
                    + " referencia un instrumento inexistente.");
        }
        return new MovimientoPatrimonio(
            movimiento.id(),
            movimiento.portafolioId(),
            movimiento.cuentaId(),
            movimiento.posicionId(),
            instrumento.id(),
            instrumento.claseActivoId(),
            instrumento.claseActivoNombre(),
            instrumento.tipoInstrumentoId(),
            instrumento.tipoInstrumentoNombre(),
            movimiento.tipo().codigo(),
            movimiento.tipo() == TipoMovimiento.REVERSION,
            movimiento.fechaOperacion(),
            instrumento.monedaId(),
            instrumento.monedaCodigo(),
            instrumento.monedaNombre(),
            instrumento.monedaSimbolo(),
            instrumento.monedaDecimales(),
            movimiento.cantidad(),
            movimiento.precioUnitario(),
            movimiento.importeBruto(),
            movimiento.comisiones(),
            movimiento.impuestos(),
            movimiento.importeNeto(),
            movimiento.costoAplicado(),
            movimiento.resultadoRealizado().orElse(null),
            movimiento.efectoCantidad(),
            movimiento.efectoCosto(),
            movimiento.movimientoOrigenId().orElse(null),
            movimiento.fechaCreacion());
    }
}
