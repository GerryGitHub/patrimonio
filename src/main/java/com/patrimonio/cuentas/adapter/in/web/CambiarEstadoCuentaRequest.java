package com.patrimonio.cuentas.adapter.in.web;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para activar o desactivar una cuenta financiera.
 */
public record CambiarEstadoCuentaRequest(
    @NotNull Boolean activo
) {
}
