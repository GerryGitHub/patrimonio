package com.patrimonio.acceso.application.command;

/**
 * Credenciales enviadas por el cliente para iniciar sesión.
 *
 * <p>El campo {@code usuario} contiene el correo electrónico. La contraseña
 * solo vive durante la verificación y nunca se registra ni se devuelve.</p>
 */
public record AutenticarUsuarioCommand(
    String usuario,
    String password
) {
}
