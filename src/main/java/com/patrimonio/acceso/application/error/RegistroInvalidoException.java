package com.patrimonio.acceso.application.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Los datos de registro no cumplen las reglas mínimas de la aplicación.
 *
 * <p>El adaptador web ya valida el contrato con Bean Validation. Esta
 * excepción protege el caso de uso cuando se invoca desde otro adaptador o
 * desde pruebas, sin acoplar la aplicación a anotaciones de framework.</p>
 */
public class RegistroInvalidoException extends DomainException {

    public RegistroInvalidoException() {
        super("VALIDACION_ENTRADA", "Los datos de registro no son válidos.");
    }
}
