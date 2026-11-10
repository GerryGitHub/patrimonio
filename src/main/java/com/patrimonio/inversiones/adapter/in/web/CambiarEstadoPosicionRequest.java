package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para activar o desactivar una posición.
 */
public record CambiarEstadoPosicionRequest(
    @NotNull Boolean activo
) {
}
