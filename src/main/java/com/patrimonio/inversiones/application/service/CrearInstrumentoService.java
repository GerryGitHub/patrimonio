package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.command.CrearInstrumentoCommand;
import com.patrimonio.inversiones.application.port.in.CrearInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.support.ValidadorRelacionesInstrumento;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del alta de instrumentos.
 *
 * <p>El catálogo es global. Se valida el tipo, la moneda, el símbolo y el ISIN
 * antes de guardar. La operación exige un usuario autenticado, pero no una
 * membresía sobre un portafolio concreto.</p>
 */
@Service
public class CrearInstrumentoService implements CrearInstrumentoUseCase {

    private final InstrumentoRepository instrumentos;
    private final ValidadorRelacionesInstrumento validador;
    private final InversionesVistas vistas;

    public CrearInstrumentoService(
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
    public InstrumentoView execute(CrearInstrumentoCommand command) {
        Objects.requireNonNull(command, "command");

        String nombre = TextosInversiones.nombre(command.nombre());
        String simbolo = TextosInversiones.simbolo(command.simbolo());
        String isin = TextosInversiones.isin(command.isin());

        validador.validarTipoYMoneda(
            command.tipoInstrumentoId(), command.monedaId());
        validador.validarUnicidad(simbolo, command.monedaId(), isin, null);

        Instrumento instrumento = Instrumento.crear(
            UUID.randomUUID(),
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

        return vistas.instrumento(instrumentos.guardar(instrumento));
    }
}
