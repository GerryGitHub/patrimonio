package com.patrimonio.inversiones.application.view;

import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de aplicación de un movimiento. Los importes se serializan como
 * cadena en el adaptador REST.
 */
public record MovimientoView(
    UUID id,
    UUID portafolioId,
    UUID cuentaId,
    String cuentaNombre,
    UUID posicionId,
    UUID instrumentoId,
    String instrumentoNombre,
    String instrumentoSimbolo,
    TipoMovimiento tipo,
    String tipoNombre,
    LocalDate fechaOperacion,
    LocalDate fechaLiquidacion,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal importeBruto,
    BigDecimal comisiones,
    BigDecimal impuestos,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    BigDecimal tipoCambio,
    BigDecimal importeNeto,
    BigDecimal costoAplicado,
    BigDecimal resultadoRealizado,
    BigDecimal cantidadResultante,
    BigDecimal costoResultante,
    String estado,
    UUID usuarioId,
    UUID movimientoOrigenId,
    boolean revertido,
    String observaciones,
    Instant fechaCreacion
) {
}
