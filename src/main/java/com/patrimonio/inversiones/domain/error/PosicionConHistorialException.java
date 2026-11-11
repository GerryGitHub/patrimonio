package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * La posición ya tiene movimientos confirmados y no admite la edición libre de
 * cantidad o costo. Las correcciones deben registrarse con movimientos de
 * ajuste o reversión.
 */
public class PosicionConHistorialException extends DomainException {

    public PosicionConHistorialException(String mensaje) {
        super("POSICION_CON_HISTORIAL", mensaje);
    }
}
