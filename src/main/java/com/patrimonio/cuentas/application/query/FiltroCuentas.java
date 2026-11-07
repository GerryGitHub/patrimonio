package com.patrimonio.cuentas.application.query;

import java.util.UUID;

/**
 * Filtros opcionales del listado de cuentas.
 */
public record FiltroCuentas(
    UUID institucionId,
    UUID tipoCuentaId,
    Boolean activo
) {
}
