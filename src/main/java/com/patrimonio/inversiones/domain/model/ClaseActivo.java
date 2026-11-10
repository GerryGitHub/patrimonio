package com.patrimonio.inversiones.domain.model;

import java.util.UUID;

/**
 * Clase de activo del catálogo extensible.
 *
 * <p>Es un modelo de solo lectura: las clases y sus tipos se administran con
 * datos iniciales o por una futura pantalla de catálogos. El dominio de
 * instrumentos solo necesita identificarlas y mostrarlas.</p>
 */
public record ClaseActivo(
    UUID id,
    String nombre,
    String descripcion,
    boolean activo
) {
}
