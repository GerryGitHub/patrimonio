package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * El movimiento no puede revertirse: por ejemplo, una reversión de otra
 * reversión.
 */
public class MovimientoNoReversibleException extends DomainException {

    public MovimientoNoReversibleException(String mensaje) {
        super("MOVIMIENTO_NO_REVERSIBLE", mensaje);
    }
}
