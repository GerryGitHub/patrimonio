package com.patrimonio.acceso.application.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Ya existe una cuenta con el correo solicitado.
 *
 * <p>El registro es la única operación que revela esta condición: el usuario
 * necesita saber que debe iniciar sesión o recuperar su cuenta. El mensaje no
 * incluye el correo enviado.</p>
 */
public class EmailDuplicadoException extends DomainException {

    public EmailDuplicadoException() {
        super("EMAIL_DUPLICADO", "Ya existe una cuenta con ese correo electrónico.");
    }
}
