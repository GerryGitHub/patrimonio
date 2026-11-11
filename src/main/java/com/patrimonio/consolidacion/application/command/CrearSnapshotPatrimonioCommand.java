package com.patrimonio.consolidacion.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Captura manual de un snapshot patrimonial.
 *
 * <p>La fecha es opcional; si no se indica se usa la fecha actual en UTC. La
 * captura es idempotente por portafolio, fecha, moneda y fuente.</p>
 */
public record CrearSnapshotPatrimonioCommand(
    UUID usuarioId,
    UUID portafolioId,
    LocalDate fecha
) {

    public CrearSnapshotPatrimonioCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
