package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * Un movimiento no cumple las invariantes del dominio o no puede aplicarse
 * sobre el estado actual de la posición.
 */
public class MovimientoInvalidoException extends DomainException {

    public MovimientoInvalidoException(String mensaje) {
        super("MOVIMIENTO_INVALIDO", mensaje);
    }
}
