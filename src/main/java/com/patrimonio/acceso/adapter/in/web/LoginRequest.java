package com.patrimonio.acceso.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales de inicio de sesión.
 *
 * <p>{@code usuario} es el correo electrónico. Los límites coinciden con las
 * columnas del esquema y con el máximo de BCrypt.</p>
 */
public record LoginRequest(
    @NotBlank @Size(max = 254) String usuario,
    @NotBlank @Size(max = 72) String password
) {
}
