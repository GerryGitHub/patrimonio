package com.patrimonio.inversiones.application.query;

import java.util.UUID;

/**
 * Consulta del detalle de un instrumento.
 */
public record ConsultarInstrumentoQuery(
    UUID instrumentoId
) {
}
