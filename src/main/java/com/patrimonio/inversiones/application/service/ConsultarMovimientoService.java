package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.port.in.ConsultarMovimientoUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.query.ConsultarMovimientoQuery;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.MovimientoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Devuelve el detalle de un movimiento del portafolio autorizado.
 */
@Service
public class ConsultarMovimientoService implements ConsultarMovimientoUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final MovimientoRepository movimientos;
    private final InversionesVistas vistas;

    public ConsultarMovimientoService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        MovimientoRepository movimientos,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.movimientos = movimientos;
        this.vistas = vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public MovimientoView execute(ConsultarMovimientoQuery query) {
        Objects.requireNonNull(query, "query");

        accesoPortafolio.exigirLectura(
            query.usuarioId(), query.portafolioId());

        Movimiento movimiento = movimientos
            .buscarPorId(query.movimientoId())
            .filter(encontrado -> encontrado.portafolioId()
                .equals(query.portafolioId()))
            .orElseThrow(() -> new NotFoundException(
                "No existe el movimiento indicado."));

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(
                query.portafolioId(), movimiento.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el movimiento indicado."));

        Instrumento instrumento = instrumentos
            .buscarPorId(movimiento.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento del movimiento."));

        boolean revertido = movimientos
            .buscarReversionDe(movimiento.id())
            .isPresent();

        return vistas.movimiento(
            movimiento,
            vistas.instrumento(instrumento),
            cuenta,
            revertido);
    }
}
