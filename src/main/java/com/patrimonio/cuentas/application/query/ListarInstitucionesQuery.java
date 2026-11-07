package com.patrimonio.cuentas.application.query;

import com.patrimonio.cuentas.domain.model.TipoInstitucion;

/**
 * Filtros opcionales del catálogo de instituciones.
 */
public record ListarInstitucionesQuery(
    TipoInstitucion tipo,
    Boolean activo
) {
}
