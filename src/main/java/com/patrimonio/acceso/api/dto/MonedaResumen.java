package com.patrimonio.acceso.api.dto;

import java.util.UUID;

/**
 * Moneda del catálogo publicada por el módulo {@code acceso}.
 *
 * <p>Es un contrato de solo lectura: no contiene entidades JPA ni anotaciones
 * de framework. El símbolo puede ser nulo porque el esquema lo permite.</p>
 */
public record MonedaResumen(
    UUID id,
    String codigo,
    String nombre,
    String simbolo,
    int decimales
) {
}
