package com.patrimonio.cuentas.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Una institución financiera no cumple las invariantes del dominio.
 */
public class InstitucionInvalidaException extends DomainException {

    public InstitucionInvalidaException(String mensaje) {
        super("INSTITUCION_INVALIDA", mensaje);
    }
}
