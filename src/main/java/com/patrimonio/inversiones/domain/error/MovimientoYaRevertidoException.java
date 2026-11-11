package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * El movimiento ya tiene una reversión confirmada.
 */
public class MovimientoYaRevertidoException extends DomainException {

    public MovimientoYaRevertidoException(String mensaje) {
        super("MOVIMIENTO_YA_REVERTIDO", mensaje);
    }
}
