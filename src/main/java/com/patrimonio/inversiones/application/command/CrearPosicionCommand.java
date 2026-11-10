package com.patrimonio.inversiones.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Datos para registrar la posición inicial de un instrumento en una cuenta.
 *
 * <p>La moneda no viaja en el comando: se deriva del instrumento. El
 * portafolio se toma de la ruta y se valida contra la cuenta.</p>
 */
public record CrearPosicionCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID cuentaId,
    UUID instrumentoId,
    BigDecimal cantidad,
    BigDecimal costoUnitario,
    LocalDate fechaApertura,
    String observaciones
) {

    public CrearPosicionCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
