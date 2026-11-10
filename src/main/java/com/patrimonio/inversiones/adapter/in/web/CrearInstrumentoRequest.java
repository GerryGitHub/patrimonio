package com.patrimonio.inversiones.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Datos para registrar un instrumento.
 */
public record CrearInstrumentoRequest(
    @NotNull UUID tipoInstrumentoId,
    @NotBlank @Size(max = 200) String nombre,
    @Size(max = 32) String simbolo,
    @Size(max = 150) String emisor,
    @Size(max = 100) String mercado,
    @NotNull UUID monedaId,
    @Size(max = 100) String identificadorExterno,
    @Size(max = 100) String isin,
    @Size(max = 500) String descripcion
) {
}
