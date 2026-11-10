package com.patrimonio.inversiones.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos para editar un instrumento existente.
 *
 * <p>El estado no forma parte de la edición: se cambia con su propia
 * operación.</p>
 */
public record ActualizarInstrumentoCommand(
    UUID usuarioId,
    UUID instrumentoId,
    UUID tipoInstrumentoId,
    String nombre,
    String simbolo,
    String emisor,
    String mercado,
    UUID monedaId,
    String identificadorExterno,
    String isin,
    String descripcion
) {

    public ActualizarInstrumentoCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(instrumentoId, "instrumentoId");
    }
}
