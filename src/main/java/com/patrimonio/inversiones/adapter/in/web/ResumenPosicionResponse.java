package com.patrimonio.inversiones.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

/**
 * Resumen de saldos de una posición. El resultado realizado puede ser negativo.
 */
public record ResumenPosicionResponse(
    UUID posicionId,
    UUID portafolioId,
    UUID cuentaId,
    UUID instrumentoId,
    String instrumentoNombre,
    String instrumentoSimbolo,
    String cantidad,
    String costoTotal,
    String costoUnitario,
    String resultadoRealizado,
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
