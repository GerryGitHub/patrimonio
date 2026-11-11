package com.patrimonio.inversiones.domain.model;

import java.math.BigDecimal;

/**
 * Datos de entrada para calcular el efecto de un movimiento sobre una posición.
 *
 * <p>Los campos {@code ...Origen} solo se usan al revertir un movimiento
 * confirmado. Para el resto de tipos son nulos.</p>
 */
public record ContextoCalculo(
    BigDecimal cantidadActual,
    BigDecimal costoActual,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal comisiones,
    BigDecimal impuestos,
    BigDecimal costoAjuste,
    BigDecimal efectoCantidadOrigen,
    BigDecimal efectoCostoOrigen,
    BigDecimal importeBrutoOrigen,
    BigDecimal importeNetoOrigen,
    BigDecimal costoAplicadoOrigen,
    BigDecimal resultadoRealizadoOrigen
) {
}
