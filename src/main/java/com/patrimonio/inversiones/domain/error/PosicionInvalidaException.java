package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Una posición no cumple las invariantes del dominio.
 */
public class PosicionInvalidaException extends DomainException {

    public PosicionInvalidaException(String mensaje) {
        super("POSICION_INVALIDA", mensaje);
    }
}
