package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.ActualizarPosicionCommand;
import com.patrimonio.inversiones.application.port.in.ActualizarPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.FechasInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la corrección de una posición existente.
 *
 * <p>La cuenta y el instrumento no cambian: la posición pertenece a la cuenta
 * en la que se registró. La cuenta se verifica contra el portafolio de la ruta
 * para impedir el acceso a posiciones de otros portafolios.</p>
 */
@Service
public class ActualizarPosicionService implements ActualizarPosicionUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final InversionesVistas vistas;

    public ActualizarPosicionService(
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
    @Transactional
    public PosicionView execute(ActualizarPosicionCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        Posicion existente = posiciones.buscarPorId(command.posicionId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(command.portafolioId(), existente.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        Instrumento instrumento = instrumentos
            .buscarPorId(existente.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento de la posición."));

        LocalDate fecha = command.fechaApertura() == null
            ? FechasInversiones.fecha(existente.fechaApertura())
            : command.fechaApertura();

        Posicion actualizada = existente.actualizarDatos(
            command.cantidad(),
            command.costoUnitario(),
            TextosInversiones.opcional(command.observaciones()),
            FechasInversiones.inicioDelDia(fecha),
            Instant.now());

        return vistas.posicion(
            posiciones.guardar(actualizada),
            vistas.instrumento(instrumento),
            cuenta);
    }
}
