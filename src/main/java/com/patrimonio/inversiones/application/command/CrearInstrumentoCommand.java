package com.patrimonio.inversiones.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Datos para registrar un instrumento en el catálogo global.
 *
 * <p>El usuario autenticado se conserva para dejar trazabilidad de la
 * operación, aunque el catálogo no pertenece a un usuario concreto.</p>
 */
public record CrearInstrumentoCommand(
    UUID usuarioId,
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

    public CrearInstrumentoCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
    }
}
