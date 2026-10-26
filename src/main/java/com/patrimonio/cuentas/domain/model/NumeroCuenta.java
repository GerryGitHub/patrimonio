package com.patrimonio.cuentas.domain.model;

import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;

/**
 * Número de cuenta financiera.
 *
 * <p>Es opcional a nivel de cuenta, pero cuando existe no puede estar vacío ni
 * superar los 64 caracteres que admite la columna
 * {@code cuenta_financiera.numero_cuenta}.</p>
 */
public record NumeroCuenta(String valor) {

    private static final int LONGITUD_MAXIMA = 64;

    public NumeroCuenta {
        if (valor == null || valor.isBlank()) {
            throw new CuentaInvalidaException("El número de cuenta no puede estar vacío.");
        }
        if (valor.length() > LONGITUD_MAXIMA) {
            throw new CuentaInvalidaException(
                "El número de cuenta no puede superar " + LONGITUD_MAXIMA + " caracteres.");
        }
    }
}
