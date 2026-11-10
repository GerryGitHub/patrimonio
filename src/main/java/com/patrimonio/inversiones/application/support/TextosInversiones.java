package com.patrimonio.inversiones.application.support;

import java.util.Locale;

/**
 * Normalización de texto común a los casos de uso del módulo.
 */
public final class TextosInversiones {

    private TextosInversiones() {
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

    public static String simbolo(String valor) {
        String normalizado = opcional(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }

    public static String isin(String valor) {
        String normalizado = opcional(valor);
        return normalizado == null ? null : normalizado.toUpperCase(Locale.ROOT);
    }
}
