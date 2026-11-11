package com.patrimonio.inversiones.adapter.in.web;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de reconstruir una posición desde su historial.
 */
public record ReconstruccionResponse(
    UUID posicionId,
    boolean coincide,
    boolean reparada,
    String cantidadCalculada,
    String costoCalculado,
    String resultadoCalculado,
    String cantidadMaterializada,
    String costoMaterializado,
    String resultadoMaterializado,
    List<String> diferencias
) {
}
