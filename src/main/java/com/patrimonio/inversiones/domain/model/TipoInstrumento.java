package com.patrimonio.inversiones.domain.model;

import java.util.UUID;

/**
 * Tipo de instrumento, siempre dentro de una clase de activo.
 *
 * <p>La clasificación es extensible: agregar una clase o un tipo no requiere
 * cambiar el agregado {@link Instrumento}, que solo guarda la referencia al
 * tipo.</p>
 */
public record TipoInstrumento(
    UUID id,
    UUID claseActivoId,
    String nombre,
    String descripcion,
    boolean activo
) {
}
