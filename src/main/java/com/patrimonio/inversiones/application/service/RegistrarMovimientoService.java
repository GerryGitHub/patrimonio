package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.RegistrarMovimientoCommand;
import com.patrimonio.inversiones.application.port.in.RegistrarMovimientoUseCase;
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
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.MotorMovimientos;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.inversiones.domain.model.SolicitudMovimiento;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra un movimiento confirmado y actualiza la posición en la misma
 * transacción.
 *
 * <p>La posición se bloquea de forma pesimista para serializar movimientos
 * concurrentes. La clave de idempotencia, si se envía, se resuelve antes de
 * escribir: un reintento devuelve el movimiento ya creado y no duplica el
 * efecto.</p>
 */
@Service
public class RegistrarMovimientoService implements RegistrarMovimientoUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final MovimientoRepository movimientos;
    private final MotorMovimientos motor;
    private final InversionesVistas vistas;

    public RegistrarMovimientoService(
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
    public MovimientoView execute(RegistrarMovimientoCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        String clave = TextosInversiones.opcional(command.claveIdempotencia());
        if (clave != null) {
            Optional<Movimiento> existente = movimientos
                .buscarPorClaveIdempotencia(command.portafolioId(), clave);
            if (existente.isPresent()) {
                return repetido(command, existente.get());
            }
        }

        LocalDate fecha = command.fechaOperacion() == null
            ? FechasInversiones.hoy()
            : command.fechaOperacion();

        Posicion posicion = posiciones
            .buscarPorIdParaActualizar(command.posicionId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(command.portafolioId(), posicion.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        if (!posicion.activo()) {
            throw new MovimientoInvalidoException(
                "La posición está inactiva y no admite movimientos.");
        }

        Instrumento instrumento = instrumentos
            .buscarPorId(posicion.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento de la posición."));

        Instant ahora = Instant.now();
        SolicitudMovimiento solicitud = new SolicitudMovimiento(
            UUID.randomUUID(),
            command.portafolioId(),
            cuenta.id(),
            posicion.id(),
            instrumento.id(),
            command.tipo(),
            fecha,
            command.fechaLiquidacion(),
            command.cantidad(),
            command.precioUnitario(),
            command.comisiones(),
            command.impuestos(),
            instrumento.monedaId(),
            command.tipoCambio(),
            command.costoTotalAjuste(),
            TextosInversiones.opcional(command.observaciones()),
            command.usuarioId(),
            clave);

        Movimiento movimiento = Movimiento.registrar(
            solicitud,
            posicion.cantidad(),
            posicion.costoTotal(),
            motor,
            ahora);

        movimientos.guardar(movimiento);

        Posicion actualizada = posicion.aplicar(
            movimiento.resultadoAplicable(),
            FechasInversiones.inicioDelDia(movimiento.fechaOperacion()),
            ahora);
        posiciones.guardar(actualizada);

        return vistas.movimiento(
            movimiento,
            vistas.instrumento(instrumento),
            cuenta,
            false);
    }

    private MovimientoView repetido(
        RegistrarMovimientoCommand command,
        Movimiento existente
    ) {
        if (existente.posicionId().equals(command.posicionId())
            && existente.tipo() == command.tipo()
            && mismosImportes(existente.cantidad(), command.cantidad())
            && mismosImportes(
                existente.precioUnitario(), command.precioUnitario())) {
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
                movimientos.buscarReversionDe(existente.id()).isPresent());
        }
        throw new IdempotenciaConflictoException(
            "La clave de idempotencia ya fue utilizada con otros datos. "
                + "Usa una clave distinta para un movimiento nuevo.");
    }

    private static boolean mismosImportes(BigDecimal actual, BigDecimal nuevo) {
        if (actual == null || nuevo == null) {
            return actual == nuevo;
        }
        return actual.compareTo(nuevo) == 0;
    }
}
