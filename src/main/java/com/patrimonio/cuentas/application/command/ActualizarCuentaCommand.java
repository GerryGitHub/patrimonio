package com.patrimonio.cuentas.application.command;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Datos para editar una cuenta financiera existente.
 */
public record ActualizarCuentaCommand(
    UUID usuarioId,
    UUID portafolioId,
    UUID cuentaId,
    UUID institucionId,
    UUID tipoCuentaId,
    UUID monedaId,
    String nombre,
    String descripcion,
    String numeroCuenta,
    LocalDate fechaApertura
) {

    public ActualizarCuentaCommand {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(portafolioId, "portafolioId");
        Objects.requireNonNull(cuentaId, "cuentaId");
    }
}
