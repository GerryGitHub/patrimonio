package com.patrimonio.cuentas.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Ya existe una institución con el mismo nombre normalizado.
 */
public class InstitucionDuplicadaException extends DomainException {

    public InstitucionDuplicadaException(String mensaje) {
        super("INSTITUCION_DUPLICADA", mensaje);
    }
}
