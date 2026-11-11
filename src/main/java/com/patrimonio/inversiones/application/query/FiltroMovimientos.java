package com.patrimonio.inversiones.application.query;

import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import java.time.LocalDate;

/**
 * Filtros del historial de movimientos.
 */
public record FiltroMovimientos(
    TipoMovimiento tipo,
    LocalDate desde,
    LocalDate hasta
) {
}
