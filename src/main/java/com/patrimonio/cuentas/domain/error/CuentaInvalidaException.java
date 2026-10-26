package com.patrimonio.cuentas.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Una cuenta financiera no cumple las invariantes del dominio.
 */
public class CuentaInvalidaException extends DomainException {

    public CuentaInvalidaException(String mensaje) {
        super("CUENTA_INVALIDA", mensaje);
    }
}
