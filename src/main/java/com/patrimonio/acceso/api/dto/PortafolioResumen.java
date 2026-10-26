package com.patrimonio.acceso.api.dto;

import java.util.UUID;

/**
 * Resumen de solo lectura de un portafolio.
 *
 * <p>Contrato publicado por el módulo {@code acceso}. No contiene entidades JPA
 * ni anotaciones de framework.</p>
 */
public record PortafolioResumen(
    UUID id,
    String nombre,
    UUID monedaBaseId,
    boolean activo
) {
}
