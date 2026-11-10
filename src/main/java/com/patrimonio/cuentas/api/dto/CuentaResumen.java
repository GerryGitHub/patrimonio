package com.patrimonio.cuentas.api.dto;

import java.util.UUID;

/**
 * Resumen de una cuenta financiera publicado para otros módulos.
 *
 * <p>No expone la entidad JPA ni el número de cuenta completo. Incluye el
 * nombre del tipo de cuenta porque las reglas de compatibilidad entre cuenta
 * e instrumento lo necesitan sin acoplarse al catálogo interno del módulo
 * {@code cuentas}.</p>
 */
public record CuentaResumen(
    UUID id,
    UUID portafolioId,
    String nombre,
    UUID institucionId,
    UUID tipoCuentaId,
    String tipoCuentaNombre,
    UUID monedaId,
    boolean activo
) {
}
