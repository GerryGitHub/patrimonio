package com.patrimonio.acceso.api.dto;

import java.util.UUID;

/**
 * Capacidad efectiva de un usuario sobre un portafolio.
 *
 * <p>No expone el rol almacenado: los módulos consumidores solo necesitan
 * saber qué pueden hacer. Todos los roles del esquema conceden lectura;
 * {@code escritura} se concede a {@code PROPIETARIO}, {@code ADMINISTRADOR} y
 * {@code EDITOR}.</p>
 */
public record PermisoPortafolio(
    UUID portafolioId,
    boolean lectura,
    boolean escritura
) {

    /**
     * Constructor de compatibilidad para consumidores anteriores a la
     * distinción entre lectura y escritura.
     */
    public PermisoPortafolio(UUID portafolioId, boolean lectura) {
        this(portafolioId, lectura, lectura);
    }
}
