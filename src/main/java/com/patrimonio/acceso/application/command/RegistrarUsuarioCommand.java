package com.patrimonio.acceso.application.command;

/**
 * Datos enviados por el cliente para crear una cuenta.
 *
 * <p>La contraseña solo vive durante el registro: se transforma en un hash
 * antes de persistirla y nunca se registra ni se devuelve por HTTP.</p>
 */
public record RegistrarUsuarioCommand(
    String nombre,
    String email,
    String password
) {
}
