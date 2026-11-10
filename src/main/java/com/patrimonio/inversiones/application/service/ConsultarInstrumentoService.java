package com.patrimonio.inversiones.application.service;

import com.patrimonio.inversiones.application.port.in.ConsultarInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.query.ConsultarInstrumentoQuery;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la consulta del detalle de un instrumento.
 */
@Service
public class ConsultarInstrumentoService implements ConsultarInstrumentoUseCase {

    private final InstrumentoRepository instrumentos;
    private final InversionesVistas vistas;

    public ConsultarInstrumentoService(
        InstrumentoRepository instrumentos,
        InversionesVistas vistas
    ) {
        this.instrumentos = instrumentos;
        this.vistas = vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public InstrumentoView execute(ConsultarInstrumentoQuery query) {
        Objects.requireNonNull(query, "query");
        return vistas.instrumento(
            instrumentos.buscarPorId(query.instrumentoId())
                .orElseThrow(() -> new NotFoundException(
                    "No existe el instrumento indicado.")));
    }
}
