package com.patrimonio.cuentas.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * La moneda indicada para la cuenta no existe o está inactiva.
 */
public class MonedaInvalidaException extends DomainException {

    public MonedaInvalidaException() {
        super(
            "MONEDA_INVALIDA",
            "La moneda de la cuenta no existe o no está activa.");
    }
}
