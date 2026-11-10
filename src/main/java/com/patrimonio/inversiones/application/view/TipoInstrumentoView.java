package com.patrimonio.inversiones.application.view;

import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import java.util.UUID;

/**
 * Vista de un tipo de instrumento con el nombre de su clase de activo.
 */
public record TipoInstrumentoView(
    UUID id,
    UUID claseActivoId,
    String claseActivoNombre,
    String nombre,
    String descripcion,
    boolean activo
) {

    public static TipoInstrumentoView from(
        TipoInstrumento tipo,
        String claseActivoNombre
    ) {
        return new TipoInstrumentoView(
            tipo.id(),
            tipo.claseActivoId(),
            claseActivoNombre,
            tipo.nombre(),
            tipo.descripcion(),
            tipo.activo());
    }
}
