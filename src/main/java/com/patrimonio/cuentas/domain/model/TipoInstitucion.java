package com.patrimonio.cuentas.domain.model;

import com.patrimonio.cuentas.domain.error.InstitucionInvalidaException;
import java.util.Locale;

/**
 * Categoría de una institución financiera.
 *
 * <p>Los valores se almacenan en {@code institucion.tipo} como texto estable.
 * No se crea una tabla de catálogo adicional: el conjunto es pequeño, cerrado
 * y forma parte del lenguaje del dominio.</p>
 */
public enum TipoInstitucion {
    BANCO,
    SOFIPO,
    CASA_BOLSA,
    EXCHANGE,
    OTRA;

    /**
     * Convierte el valor almacenado o recibido a la categoría correspondiente.
     *
     * @throws InstitucionInvalidaException si falta o no es una categoría
     *     admitida
     */
    public static TipoInstitucion desde(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new InstitucionInvalidaException(
                "La categoría de la institución es obligatoria.");
        }
        try {
            return valueOf(valor.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InstitucionInvalidaException(
                "La categoría de la institución no es válida.");
        }
    }
}
