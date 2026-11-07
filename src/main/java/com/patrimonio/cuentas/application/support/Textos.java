package com.patrimonio.cuentas.application.support;

/**
 * Normalización de texto común a los casos de uso del módulo.
 *
 * <p>Los nombres se recortan y se colapsan los espacios interiores para evitar
 * duplicados visualmente equivalentes. Las referencias bancarias solo se
 * recortan: sus espacios interiores pueden ser significativos.</p>
 */
public final class Textos {

    private Textos() {
    }

    public static String nombre(String valor) {
        return valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
    }

    public static String opcional(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.strip();
        return normalizado.isEmpty() ? null : normalizado;
    }

    public static String referencia(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.strip();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
