package com.patrimonio.acceso.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Datos del formulario de creación de portafolios.
 *
 * <p>Los límites coinciden con las columnas del esquema. La descripción es
 * opcional; el caso de uso la normaliza y convierte una cadena vacía en
 * {@code null}.</p>
 */
public record CrearPortafolioRequest(
    @NotBlank @Size(max = 150) String nombre,
    @Size(max = 500) String descripcion,
    @NotNull UUID monedaBaseId
) {
}
