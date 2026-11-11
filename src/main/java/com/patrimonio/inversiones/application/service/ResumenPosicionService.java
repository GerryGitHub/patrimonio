package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.port.in.ResumenPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.query.ResumenPosicionQuery;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.application.view.ResumenPosicionView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devuelve el resumen de saldos de una posición.
 *
 * <p>La cantidad y el costo provienen del estado materializado; el resultado
 * realizado es la suma acumulada de las ventas y reversiones. No se calcula
 * valor de mercado ni rendimiento no realizado.</p>
 */
@Service
public class ResumenPosicionService implements ResumenPosicionUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final MovimientoRepository movimientos;
    private final InversionesVistas vistas;

    public ResumenPosicionService(
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
    public ResumenPosicionView execute(ResumenPosicionQuery query) {
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

        InstrumentoView vistaInstrumento = vistas.instrumento(instrumento);

        return new ResumenPosicionView(
            posicion.id(),
            query.portafolioId(),
            cuenta.id(),
            instrumento.id(),
            vistaInstrumento.nombre(),
            vistaInstrumento.simbolo(),
            posicion.cantidad(),
            posicion.costoTotal(),
            posicion.costoUnitario(),
            posicion.resultadoRealizado(),
            vistaInstrumento.monedaId(),
            vistaInstrumento.monedaCodigo(),
            vistaInstrumento.monedaNombre(),
            vistaInstrumento.monedaSimbolo(),
            vistaInstrumento.monedaDecimales(),
            posicion.fechaUltimoMovimiento().orElse(null),
            movimientos.listarPorPosicion(posicion.id()).size(),
            posicion.activo());
    }
}
