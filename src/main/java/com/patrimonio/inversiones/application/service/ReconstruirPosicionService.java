package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.ReconstruirPosicionCommand;
import com.patrimonio.inversiones.application.port.in.ReconstruirPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.FechasInversiones;
import com.patrimonio.inversiones.application.view.ReconstruccionView;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconstruye el estado de una posición a partir de sus movimientos.
 *
 * <p>Nunca modifica ni elimina movimientos. Si el estado materializado no
 * coincide con el historial, la reconstrucción lo informa y, solo cuando el
 * llamador lo solicita, repara la posición. La reparación queda registrada en
 * la fecha de actualización y en la versión de la posición.</p>
 */
@Service
public class ReconstruirPosicionService implements ReconstruirPosicionUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final PosicionRepository posiciones;
    private final MovimientoRepository movimientos;

    public ReconstruirPosicionService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        PosicionRepository posiciones,
        MovimientoRepository movimientos
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
    }

    @Override
    @Transactional
    public ReconstruccionView execute(ReconstruirPosicionCommand command) {
        Objects.requireNonNull(command, "command");

        if (command.reparar()) {
            accesoPortafolio.exigirEscritura(
                command.usuarioId(), command.portafolioId());
        } else {
            accesoPortafolio.exigirLectura(
                command.usuarioId(), command.portafolioId());
        }

        Posicion posicion = (command.reparar()
            ? posiciones.buscarPorIdParaActualizar(command.posicionId())
            : posiciones.buscarPorId(command.posicionId()))
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(command.portafolioId(), posicion.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la posición indicada."));

        List<Movimiento> historial = movimientos
            .listarPorPosicion(posicion.id());

        BigDecimal cantidad = BigDecimal.ZERO;
        BigDecimal costo = BigDecimal.ZERO;
        BigDecimal resultado = BigDecimal.ZERO;
        LocalDate ultimaFecha = null;
        List<String> diferencias = new ArrayList<>();

        for (Movimiento movimiento : historial) {
            cantidad = cantidad.add(movimiento.efectoCantidad());
            costo = costo.add(movimiento.efectoCosto());
            resultado = resultado.add(
                movimiento.resultadoRealizado().orElse(BigDecimal.ZERO));
            if (ultimaFecha == null
                || movimiento.fechaOperacion().isAfter(ultimaFecha)) {
                ultimaFecha = movimiento.fechaOperacion();
            }
            if (movimiento.cantidadResultante().compareTo(cantidad) != 0) {
                diferencias.add(
                    "El movimiento " + movimiento.id()
                        + " declara una cantidad resultante "
                        + movimiento.cantidadResultante().toPlainString()
                        + " pero el historial acumula "
                        + cantidad.toPlainString() + ".");
            }
            if (movimiento.costoResultante().compareTo(costo) != 0) {
                diferencias.add(
                    "El movimiento " + movimiento.id()
                        + " declara un costo resultante "
                        + movimiento.costoResultante().toPlainString()
                        + " pero el historial acumula "
                        + costo.toPlainString() + ".");
            }
        }

        if (posicion.cantidad().compareTo(cantidad) != 0) {
            diferencias.add(
                "La cantidad materializada "
                    + posicion.cantidad().toPlainString()
                    + " no coincide con la reconstruida "
                    + cantidad.toPlainString() + ".");
        }
        if (posicion.costoTotal().compareTo(costo) != 0) {
            diferencias.add(
                "El costo materializado "
                    + posicion.costoTotal().toPlainString()
                    + " no coincide con el reconstruido "
                    + costo.toPlainString() + ".");
        }
        if (posicion.resultadoRealizado().compareTo(resultado) != 0) {
            diferencias.add(
                "El resultado realizado materializado "
                    + posicion.resultadoRealizado().toPlainString()
                    + " no coincide con el reconstruido "
                    + resultado.toPlainString() + ".");
        }

        boolean coincide = diferencias.isEmpty();
        boolean reparada = false;
        if (!coincide && command.reparar()) {
            Instant fechaUltimo = ultimaFecha == null
                ? null
                : FechasInversiones.inicioDelDia(ultimaFecha);
            posiciones.guardar(posicion.reparar(
                cantidad,
                costo,
                resultado,
                fechaUltimo,
                Instant.now()));
            reparada = true;
        }

        return new ReconstruccionView(
            posicion.id(),
            coincide,
            reparada,
            cantidad,
            costo,
            resultado,
            posicion.cantidad(),
            posicion.costoTotal(),
            posicion.resultadoRealizado(),
            List.copyOf(diferencias));
    }
}
