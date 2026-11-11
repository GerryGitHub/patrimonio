package com.patrimonio.inversiones.application.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Resultado de reconstruir una posición desde su historial.
 *
 * <p>No modifica los movimientos. Si {@code reparada} es verdadero, solo se
 * actualizó el estado materializado de la posición.</p>
 */
public record ReconstruccionView(
    UUID posicionId,
    boolean coincide,
    boolean reparada,
    BigDecimal cantidadCalculada,
    BigDecimal costoCalculado,
    BigDecimal resultadoCalculado,
    BigDecimal cantidadMaterializada,
    BigDecimal costoMaterializado,
    BigDecimal resultadoMaterializado,
    List<String> diferencias
) {
}
