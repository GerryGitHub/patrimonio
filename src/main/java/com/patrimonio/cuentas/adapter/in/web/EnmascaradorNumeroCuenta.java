package com.patrimonio.cuentas.adapter.in.web;

/**
 * Enmascara el número de cuenta antes de serializarlo en la API.
 *
 * <p>El valor completo se conserva en el dominio y en la persistencia para
 * operaciones internas, pero nunca se devuelve por HTTP ni se registra en
 * logs. La máscara muestra solo los últimos cuatro caracteres; los números de
 * cuatro caracteres o menos se ocultan por completo.</p>
 */
final class EnmascaradorNumeroCuenta {

    private static final int CARACTERES_VISIBLES = 4;

    private EnmascaradorNumeroCuenta() {
    }

    /**
     * Devuelve el número enmascarado o {@code null} si no hay valor.
     *
     * <p>No lanza excepciones con valores nulos, vacíos, en blanco o muy
     * cortos. Los espacios exteriores se ignoran para no filtrar el valor
     * original.</p>
     */
    static String enmascarar(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            return null;
        }

        String valor = numeroCuenta.strip();
        if (valor.length() <= CARACTERES_VISIBLES) {
            return "*".repeat(valor.length());
        }

        return "****" + valor.substring(valor.length() - CARACTERES_VISIBLES);
    }
}
