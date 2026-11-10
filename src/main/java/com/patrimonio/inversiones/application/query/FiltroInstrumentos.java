package com.patrimonio.inversiones.application.query;

import java.util.UUID;

/**
 * Filtros opcionales del catálogo de instrumentos.
 *
 * <p>La búsqueda libre se aplica sobre nombre, símbolo, emisor, mercado,
 * identificador externo e ISIN.</p>
 */
public record FiltroInstrumentos(
    String busqueda,
    UUID claseActivoId,
    UUID tipoInstrumentoId,
    Boolean activo
) {
}
