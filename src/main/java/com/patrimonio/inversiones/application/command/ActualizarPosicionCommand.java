package com.patrimonio.inversiones.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Datos para corregir una posición existente.
 *
 * <p>La cuenta, el instrumento y la moneda no cambian: una posición pertenece
 * a una cuenta concreta. Para moverla a otra cuenta se registra una posición
 * nueva.</p>
 */
public record ActualizarPosicionCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID posicionId,
    BigDecimal cantidad,
    BigDecimal costoUnitario,
    LocalDate fechaApertura,
    String observaciones
) {

    public ActualizarPosicionCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(posicionId, "posicionId");
    }
}
