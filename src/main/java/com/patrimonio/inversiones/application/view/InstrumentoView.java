package com.patrimonio.inversiones.application.view;

import java.time.Instant;
import java.util.UUID;

/**
 * Vista de aplicación de un instrumento.
 *
 * <p>Incluye los nombres de la clase de activo, el tipo de instrumento y la
 * moneda para que la interfaz no tenga que resolverlos con consultas
 * adicionales. Los importes no forman parte de esta vista.</p>
 */
public record InstrumentoView(
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
