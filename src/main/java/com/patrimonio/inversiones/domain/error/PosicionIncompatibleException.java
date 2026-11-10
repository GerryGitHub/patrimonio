package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * La cuenta y el instrumento no son compatibles según las reglas del módulo.
 *
 * <p>Por ejemplo, una cuenta de exchange solo admite activos digitales y una
 * cuenta a plazo no admite posiciones bursátiles porque corresponde a un
 * contrato de inversión de una entrega posterior.</p>
 */
public class PosicionIncompatibleException extends DomainException {

    public PosicionIncompatibleException(String mensaje) {
        super("POSICION_INCOMPATIBLE", mensaje);
    }
}
