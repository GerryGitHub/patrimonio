package com.patrimonio.cuentas.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * El tipo de cuenta no existe o está inactivo.
 */
public class TipoCuentaInvalidoException extends DomainException {

    public TipoCuentaInvalidoException(String mensaje) {
        super("TIPO_CUENTA_INVALIDO", mensaje);
    }
}
