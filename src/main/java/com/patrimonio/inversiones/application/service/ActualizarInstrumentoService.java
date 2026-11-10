package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.command.ActualizarInstrumentoCommand;
import com.patrimonio.inversiones.application.port.in.ActualizarInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.support.ValidadorRelacionesInstrumento;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la edición de instrumentos.
 *
 * <p>El estado se cambia con su propia operación. El símbolo y el ISIN se
 * validan contra el resto del catálogo, excluyendo el propio instrumento.</p>
 */
@Service
public class ActualizarInstrumentoService implements ActualizarInstrumentoUseCase {

    private final InstrumentoRepository instrumentos;
    private final ValidadorRelacionesInstrumento validador;
    private final InversionesVistas vistas;

    public ActualizarInstrumentoService(
        InstrumentoRepository instrumentos,
        ValidadorRelacionesInstrumento validador,
        InversionesVistas vistas
    ) {
        this.instrumentos = instrumentos;
        this.validador = validador;
        this.vistas = vistas;
    }

    @Override
    @Transactional
    public InstrumentoView execute(ActualizarInstrumentoCommand command) {
        Objects.requireNonNull(command, "command");

        Instrumento existente = instrumentos.buscarPorId(command.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento indicado."));

        String nombre = TextosInversiones.nombre(command.nombre());
        String simbolo = TextosInversiones.simbolo(command.simbolo());
        String isin = TextosInversiones.isin(command.isin());

        validador.validarTipoYMoneda(
            command.tipoInstrumentoId(), command.monedaId());
        validador.validarUnicidad(
            simbolo, command.monedaId(), isin, command.instrumentoId());

        Instrumento actualizado = existente.actualizarDatos(
            command.tipoInstrumentoId(),
            nombre,
            simbolo,
            TextosInversiones.opcional(command.emisor()),
            TextosInversiones.opcional(command.mercado()),
            command.monedaId(),
            TextosInversiones.opcional(command.identificadorExterno()),
            isin,
            TextosInversiones.opcional(command.descripcion()),
            Instant.now());

        return vistas.instrumento(instrumentos.guardar(actualizado));
    }
}
