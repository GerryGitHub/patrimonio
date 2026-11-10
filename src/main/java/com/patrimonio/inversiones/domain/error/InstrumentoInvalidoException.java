package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Un instrumento no cumple las invariantes del dominio.
 */
public class InstrumentoInvalidoException extends DomainException {

    public InstrumentoInvalidoException(String mensaje) {
        super("INSTRUMENTO_INVALIDO", mensaje);
    }
}
