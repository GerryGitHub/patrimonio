package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.command.CambiarEstadoInstrumentoCommand;
import com.patrimonio.inversiones.application.port.in.CambiarEstadoInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del cambio de estado de un instrumento.
 *
 * <p>Desactivar no elimina el instrumento: las posiciones históricas siguen
 * apuntando a él y se conserva su trazabilidad.</p>
 */
@Service
public class CambiarEstadoInstrumentoService
    implements CambiarEstadoInstrumentoUseCase {

    private final InstrumentoRepository instrumentos;
    private final InversionesVistas vistas;

    public CambiarEstadoInstrumentoService(
        InstrumentoRepository instrumentos,
        InversionesVistas vistas
    ) {
        this.instrumentos = instrumentos;
        this.vistas = vistas;
    }

    @Override
    @Transactional
    public InstrumentoView execute(CambiarEstadoInstrumentoCommand command) {
        Objects.requireNonNull(command, "command");

        Instrumento existente = instrumentos.buscarPorId(command.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento indicado."));

        return vistas.instrumento(
            instrumentos.guardar(
                existente.cambiarEstado(command.activo(), Instant.now())));
    }
}
