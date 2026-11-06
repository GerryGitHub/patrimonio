package com.patrimonio.acceso.application.command;

import java.util.UUID;

/**
 * Datos enviados por el cliente para crear un portafolio.
 *
 * <p>El identificador del usuario no viene del cuerpo HTTP: lo aporta el
 * adaptador de entrada a partir de la sesión autenticada.</p>
 */
public record CrearPortafolioCommand(
    UUID usuarioId,
    String nombre,
    String descripcion,
    UUID monedaBaseId
) {
}
