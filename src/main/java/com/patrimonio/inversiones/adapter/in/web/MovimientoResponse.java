package com.patrimonio.inversiones.adapter.in.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de un movimiento. Los importes viajan como cadena para conservar la
 * precisión de {@code NUMERIC}.
 */
public record MovimientoResponse(
    UUID id,
    UUID portafolioId,
    UUID cuentaId,
    String cuentaNombre,
    UUID posicionId,
    UUID instrumentoId,
    String instrumentoNombre,
    String instrumentoSimbolo,
    String tipo,
    String tipoNombre,
    LocalDate fechaOperacion,
    LocalDate fechaLiquidacion,
    String cantidad,
    String precioUnitario,
    String importeBruto,
    String comisiones,
    String impuestos,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    String tipoCambio,
    String importeNeto,
    String costoAplicado,
    String resultadoRealizado,
    String cantidadResultante,
    String costoResultante,
    String estado,
    UUID usuarioId,
    UUID movimientoOrigenId,
    boolean revertido,
    String observaciones,
    Instant fechaCreacion
) {
}
