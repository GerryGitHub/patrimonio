package com.patrimonio.inversiones.adapter.in.web;

import com.patrimonio.inversiones.application.view.InstrumentoView;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte vistas de instrumentos en DTOs de respuesta REST.
 */
@Component
public class InstrumentoResponseAssembler {

    public InstrumentoResponse toResponse(InstrumentoView vista) {
        return new InstrumentoResponse(
            vista.id(),
            vista.tipoInstrumentoId(),
            vista.tipoInstrumentoNombre(),
            vista.claseActivoId(),
            vista.claseActivoNombre(),
            vista.nombre(),
            vista.simbolo(),
            vista.emisor(),
            vista.mercado(),
            vista.monedaId(),
            vista.monedaCodigo(),
            vista.monedaNombre(),
            vista.monedaSimbolo(),
            vista.monedaDecimales(),
            vista.identificadorExterno(),
            vista.isin(),
            vista.descripcion(),
            vista.activo(),
            vista.fechaCreacion(),
            vista.fechaActualizacion());
    }

    public List<InstrumentoResponse> toResponses(List<InstrumentoView> vistas) {
        return vistas.stream().map(this::toResponse).toList();
    }
}
