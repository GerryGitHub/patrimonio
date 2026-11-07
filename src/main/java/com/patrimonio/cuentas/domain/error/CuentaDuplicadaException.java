package com.patrimonio.cuentas.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Ya existe una cuenta con el mismo número en la misma institución.
 */
public class CuentaDuplicadaException extends DomainException {

    public CuentaDuplicadaException(String mensaje) {
        super("CUENTA_DUPLICADA", mensaje);
    }
}
