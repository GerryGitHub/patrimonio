package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.port.in.ConsultarPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.query.ConsultarPosicionQuery;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la consulta del detalle de una posición.
 */
@Service
public class ConsultarPosicionService implements ConsultarPosicionUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final InversionesVistas vistas;

    public ConsultarPosicionService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        PosicionRepository posiciones,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.posiciones = posiciones;
        this.vistas = vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public PosicionView execute(ConsultarPosicionQuery query) {
        Objects.requireNonNull(query, "query");

        accesoPortafolio.exigirLectura(query.usuarioId(), query.portafolioId());

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

        return vistas.posicion(
            posicion,
            vistas.instrumento(instrumento),
            cuenta);
    }
}
