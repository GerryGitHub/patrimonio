package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Ya existe un instrumento con el mismo símbolo o identificador.
 */
public class InstrumentoDuplicadoException extends DomainException {

    public InstrumentoDuplicadoException(String mensaje) {
        super("INSTRUMENTO_DUPLICADO", mensaje);
    }
}
