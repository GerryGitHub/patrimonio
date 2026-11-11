package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.RevertirMovimientoCommand;
import com.patrimonio.inversiones.application.port.in.RevertirMovimientoUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.FechasInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.view.MovimientoView;
import com.patrimonio.inversiones.domain.error.IdempotenciaConflictoException;
import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import com.patrimonio.inversiones.domain.error.MovimientoYaRevertidoException;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.MotorMovimientos;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revierte un movimiento confirmado creando un movimiento compensatorio.
 *
 * <p>El movimiento original nunca se modifica ni se elimina. La reversión
 * apunta al original con {@code movimientoOrigenId} y aplica exactamente los
 * efectos inversos. Una posición solo puede revertirse una vez por movimiento
 * original.</p>
 */
@Service
public class RevertirMovimientoService implements RevertirMovimientoUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final MovimientoRepository movimientos;
    private final MotorMovimientos motor;
    private final InversionesVistas vistas;

    public RevertirMovimientoService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        PosicionRepository posiciones,
        MovimientoRepository movimientos,
        MotorMovimientos motor,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.motor = motor;
        this.vistas = vistas;
    }

    @Override
    @Transactional
    public MovimientoView execute(RevertirMovimientoCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        String clave = TextosInversiones.opcional(command.claveIdempotencia());
        if (clave != null) {
            Optional<Movimiento> repetida = movimientos
                .buscarPorClaveIdempotencia(command.portafolioId(), clave);
            if (repetida.isPresent()) {
                return repetido(command, repetida.get());
            }
        }

        Movimiento original = movimientos
            .buscarPorId(command.movimientoId())
            .filter(movimiento -> movimiento.portafolioId()
                .equals(command.portafolioId()))
            .orElseThrow(() -> new NotFoundException(
                "No existe el movimiento indicado."));

        Posicion posicion = posiciones
            .buscarPorIdParaActualizar(original.posicionId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición del movimiento."));

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(command.portafolioId(), posicion.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición del movimiento."));

        if (!posicion.activo()) {
            throw new MovimientoInvalidoException(
                "La posición está inactiva y no admite movimientos.");
        }

        if (movimientos.buscarReversionDe(original.id()).isPresent()) {
            throw new MovimientoYaRevertidoException(
                "El movimiento ya tiene una reversión confirmada.");
        }

        Instrumento instrumento = instrumentos
            .buscarPorId(posicion.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento de la posición."));

        LocalDate fecha = command.fechaOperacion() == null
            ? FechasInversiones.hoy()
            : command.fechaOperacion();
        Instant ahora = Instant.now();

        Movimiento reversion = Movimiento.reversion(
            original,
            UUID.randomUUID(),
            fecha,
            TextosInversiones.opcional(command.observaciones()),
            command.usuarioId(),
            clave,
            posicion.cantidad(),
            posicion.costoTotal(),
            motor,
            ahora);

        movimientos.guardar(reversion);

        Posicion actualizada = posicion.aplicar(
            reversion.resultadoAplicable(),
            FechasInversiones.inicioDelDia(reversion.fechaOperacion()),
            ahora);
        posiciones.guardar(actualizada);

        return vistas.movimiento(
            reversion,
            vistas.instrumento(instrumento),
            cuenta,
            false);
    }

    private MovimientoView repetido(
        RevertirMovimientoCommand command,
        Movimiento existente
    ) {
        if (existente.esReversion()
            && existente.movimientoOrigenId()
                .filter(command.movimientoId()::equals)
                .isPresent()) {
            CuentaResumen cuenta = cuentas
                .buscarEnPortafolio(
                    command.portafolioId(), existente.cuentaId())
                .orElseThrow(() -> new NotFoundException(
                    "No existe la posición del movimiento idempotente."));
            Instrumento instrumento = instrumentos
                .buscarPorId(existente.instrumentoId())
                .orElseThrow(() -> new NotFoundException(
                    "No existe el instrumento del movimiento idempotente."));
            return vistas.movimiento(
                existente,
                vistas.instrumento(instrumento),
                cuenta,
                false);
        }
        throw new IdempotenciaConflictoException(
            "La clave de idempotencia ya fue utilizada con otros datos. "
                + "Usa una clave distinta para una reversión nueva.");
    }
}
