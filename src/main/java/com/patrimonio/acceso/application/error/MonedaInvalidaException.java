package com.patrimonio.acceso.application.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * La moneda base indicada no existe o está inactiva.
 *
 * <p>El mensaje no repite el identificador enviado ni revela el catálogo
 * interno.</p>
 */
public class MonedaInvalidaException extends DomainException {

    public MonedaInvalidaException() {
        super(
            "MONEDA_INVALIDA",
            "La moneda base no existe o no está activa.");
    }
}
