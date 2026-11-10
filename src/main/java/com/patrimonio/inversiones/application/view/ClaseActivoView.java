package com.patrimonio.inversiones.application.view;

import com.patrimonio.inversiones.domain.model.ClaseActivo;
import java.util.UUID;

/**
 * Vista de una clase de activo del catálogo.
 */
public record ClaseActivoView(
    UUID id,
    String nombre,
    String descripcion,
    boolean activo
) {

    public static ClaseActivoView from(ClaseActivo clase) {
        return new ClaseActivoView(
            clase.id(),
            clase.nombre(),
            clase.descripcion(),
            clase.activo());
    }
}
