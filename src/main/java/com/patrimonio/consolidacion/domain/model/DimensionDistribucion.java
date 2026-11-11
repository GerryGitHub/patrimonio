package com.patrimonio.consolidacion.domain.model;

/**
 * Dimensión de agrupación de la distribución patrimonial.
 *
 * <p>La clase de activo y el tipo de instrumento provienen de los catálogos
 * extensibles del módulo {@code inversiones}; no se duplican como enums
 * rígidos. La institución y la cuenta se resuelven desde el módulo
 * {@code cuentas}.</p>
 */
public enum DimensionDistribucion {

    CLASE_ACTIVO,
    TIPO_INSTRUMENTO,
    INSTITUCION,
    CUENTA,
    MONEDA;

    public static DimensionDistribucion desdeCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return CLASE_ACTIVO;
        }
        String normalizado = codigo.strip().toUpperCase();
        for (DimensionDistribucion dimension : values()) {
            if (dimension.name().equals(normalizado)) {
                return dimension;
            }
        }
        throw new IllegalArgumentException(
            "Dimensión de distribución desconocida: " + codigo);
    }
}
