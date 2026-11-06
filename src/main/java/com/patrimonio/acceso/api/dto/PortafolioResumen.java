package com.patrimonio.acceso.api.dto;

import java.util.UUID;

/**
 * Resumen de solo lectura de un portafolio.
 *
 * <p>Contrato publicado por el módulo {@code acceso}. Incluye la descripción y
 * la moneda base para que el cliente pueda presentarlas sin consultar el
 * catálogo por separado. No contiene entidades JPA ni anotaciones de
 * framework.</p>
 */
public record PortafolioResumen(
    UUID id,
    String nombre,
    String descripcion,
    MonedaResumen monedaBase,
    boolean activo
) {

    /**
     * Constructor plano que usan las proyecciones JPQL.
     *
     * <p>JPQL no puede construir un objeto anidado, así que la consulta
     * entrega las columnas de la moneda y este constructor arma el resumen.</p>
     */
    public PortafolioResumen(
        UUID id,
        String nombre,
        String descripcion,
        UUID monedaBaseId,
        String monedaBaseCodigo,
        String monedaBaseNombre,
        String monedaBaseSimbolo,
        int monedaBaseDecimales,
        boolean activo
    ) {
        this(
            id,
            nombre,
            descripcion,
            new MonedaResumen(
                monedaBaseId,
                monedaBaseCodigo,
                monedaBaseNombre,
                monedaBaseSimbolo,
                monedaBaseDecimales),
            activo);
    }

    public UUID monedaBaseId() {
        return monedaBase.id();
    }
}
