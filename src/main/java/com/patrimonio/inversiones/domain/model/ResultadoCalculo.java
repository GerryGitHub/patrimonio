package com.patrimonio.inversiones.domain.model;

import java.math.BigDecimal;

/**
 * Resultado de aplicar una estrategia de movimiento.
 *
 * <p>{@code efectoCantidad} y {@code efectoCosto} son los deltas firmados que
 * se aplican a la posición materializada. Los importes son magnitudes no
 * negativas; el resultado realizado sí puede ser negativo.</p>
 */
public record ResultadoCalculo(
    BigDecimal importeBruto,
    BigDecimal importeNeto,
    BigDecimal costoAplicado,
    BigDecimal resultadoRealizado,
    BigDecimal efectoCantidad,
    BigDecimal efectoCosto,
    BigDecimal cantidadResultante,
    BigDecimal costoResultante
) {
}
