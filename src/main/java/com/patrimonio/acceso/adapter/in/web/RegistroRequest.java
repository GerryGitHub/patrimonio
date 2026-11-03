package com.patrimonio.acceso.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos del formulario de registro.
 *
 * <p>Los límites coinciden con las columnas del esquema. El formato del correo
 * se valida en el caso de uso, después de eliminar los espacios exteriores:
 * así una entrada como {@code "  Maria@Example.com  "} se normaliza en lugar
 * de rechazarse por el espacio. La contraseña se limita a 72 caracteres porque
 * BCrypt solo considera los primeros 72 bytes; el caso de uso vuelve a
 * comprobarlo en bytes para entradas no ASCII.</p>
 */
public record RegistroRequest(
    @NotBlank @Size(max = 150) String nombre,
    @NotBlank @Size(max = 254) String email,
    @NotBlank @Size(min = 8, max = 72) String password
) {
}
