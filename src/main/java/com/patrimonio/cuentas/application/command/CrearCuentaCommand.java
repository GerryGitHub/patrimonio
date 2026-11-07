package com.patrimonio.cuentas.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Datos para crear una cuenta financiera dentro de un portafolio.
 */
public record CrearCuentaCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID institucionId,
    UUID tipoCuentaId,
    UUID monedaId,
    String nombre,
    String descripcion,
    String numeroCuenta,
    LocalDate fechaApertura
) {

    public CrearCuentaCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
    }
}
