package com.patrimonio.acceso.application.error;

/**
 * Las credenciales no permiten autenticar al usuario.
 *
 * <p>Se lanza tanto si el usuario no existe como si la contraseña es
 * incorrecta o la cuenta está inactiva. El adaptador HTTP devuelve siempre la
 * misma respuesta para no revelar cuál de los casos ocurrió.</p>
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas.");
    }
}
