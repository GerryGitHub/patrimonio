package com.patrimonio.inversiones.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de respuesta de un instrumento.
 *
 * <p>Incluye los nombres de la clase de activo, el tipo y la moneda para que
 * el cliente no tenga que resolverlos. No expone entidades JPA.</p>
 */
public record InstrumentoResponse(
    UUID id,
    UUID tipoInstrumentoId,
    String tipoInstrumentoNombre,
    UUID claseActivoId,
    String claseActivoNombre,
    String nombre,
    String simbolo,
    String emisor,
    String mercado,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    String identificadorExterno,
    String isin,
    String descripcion,
    boolean activo,
    Instant fechaCreacion,
    Instant fechaActualizacion
) {
}
