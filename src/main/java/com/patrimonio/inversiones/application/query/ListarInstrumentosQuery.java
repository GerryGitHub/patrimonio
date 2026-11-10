package com.patrimonio.inversiones.application.query;

import java.util.UUID;

/**
 * Consulta del catálogo de instrumentos con filtros opcionales.
 */
public record ListarInstrumentosQuery(
    String busqueda,
    UUID claseActivoId,
    UUID tipoInstrumentoId,
    Boolean activo
) {

    public FiltroInstrumentos filtro() {
        return new FiltroInstrumentos(busqueda, claseActivoId, tipoInstrumentoId, activo);
    }
}
