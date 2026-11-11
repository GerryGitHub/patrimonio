package com.patrimonio.inversiones.application.query;

import com.patrimonio.inversiones.domain.model.Movimiento;
import java.util.List;

/**
 * Página de movimientos independiente de Spring Data.
 */
public record PaginaMovimientos(
    List<Movimiento> items,
    long total
) {
}
