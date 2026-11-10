package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.CambiarEstadoPosicionCommand;
import com.patrimonio.inversiones.application.port.in.CambiarEstadoPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del cambio de estado de una posición.
 *
 * <p>Desactivar no elimina la posición: se conserva para el historial y para
 * los agregados del portafolio. La posición se verifica contra el portafolio
 * de la ruta.</p>
 */
@Service
public class CambiarEstadoPosicionService implements CambiarEstadoPosicionUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final InversionesVistas vistas;

    public CambiarEstadoPosicionService(
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
    public PosicionView execute(CambiarEstadoPosicionCommand command) {
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

        return vistas.posicion(
            posiciones.guardar(
                existente.cambiarEstado(command.activo(), Instant.now())),
            vistas.instrumento(instrumento),
            cuenta);
    }
}
