package com.patrimonio.inversiones.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resumen de saldos de una posición derivado de sus movimientos.
 */
public record ResumenPosicionView(
    UUID posicionId,
    UUID portafolioId,
    UUID cuentaId,
    UUID instrumentoId,
    String instrumentoNombre,
    String instrumentoSymbol,
    BigDecimal cantidad,
    BigDecimal costoTotal,
    BigDecimal costoUnitario,
    BigDecimal resultadoRealizado,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    Instant fechaUltimoMovimiento,
    long movimientosConfirmados,
    boolean activo
) {
}
