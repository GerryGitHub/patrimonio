package com.patrimonio.acceso.api.dto;

import java.util.UUID;

/**
 * Capacidad efectiva de un usuario sobre un portafolio.
 *
 * <p>No expone el rol almacenado: los módulos consumidores solo necesitan
 * saber qué pueden hacer. En esta fase todos los roles del esquema conceden
 * lectura, por lo que {@code lectura} es {@code true} cuando existe membresía
 * y el portafolio está activo.</p>
 */
public record PermisoPortafolio(
    UUID portafolioId,
    boolean lectura
) {
}
