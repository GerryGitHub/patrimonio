package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.NotNull;

/**
 * Datos para activar o desactivar un instrumento.
 */
public record CambiarEstadoInstrumentoRequest(
    @NotNull Boolean activo
) {
}
