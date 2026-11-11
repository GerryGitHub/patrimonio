package com.patrimonio.inversiones.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de entrada para registrar un movimiento.
 *
 * <p>Es un objeto de dominio, no un DTO web: el adaptador REST lo construye a
 * partir de su propio request. Los importes derivados no se aceptan desde el
 * cliente.</p>
 */
public record SolicitudMovimiento(
    UUID id,
    UUID portafolioId,
    UUID cuentaId,
    UUID posicionId,
    UUID instrumentoId,
    TipoMovimiento tipo,
    LocalDate fechaOperacion,
    LocalDate fechaLiquidacion,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal comisiones,
    BigDecimal impuestos,
    UUID monedaId,
    BigDecimal tipoCambio,
    BigDecimal costoTotalAjuste,
    String observaciones,
    UUID usuarioId,
    String claveIdempotencia
) {
}
