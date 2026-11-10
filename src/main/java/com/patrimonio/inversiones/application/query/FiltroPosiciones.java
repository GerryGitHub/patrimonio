package com.patrimonio.inversiones.application.query;

import java.util.UUID;

/**
 * Filtros opcionales del listado de posiciones.
 *
 * <p>La clase de activo se resuelve en la aplicación a partir del tipo de
 * instrumento, porque una posición solo guarda la referencia al instrumento.</p>
 */
public record FiltroPosiciones(
    UUID cuentaId,
    UUID instrumentoId,
    UUID claseActivoId,
    Boolean activo
) {
}
