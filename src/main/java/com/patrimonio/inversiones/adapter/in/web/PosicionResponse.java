package com.patrimonio.inversiones.adapter.in.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de respuesta de una posición.
 *
 * <p>Los importes viajan como cadena para conservar exactamente la precisión
 * decimal de {@code NUMERIC}. El costo total es {@code cantidad × costo
 * unitario}; no es valor de mercado ni incluye rendimiento.</p>
 */
public record PosicionResponse(
    UUID id,
    UUID cuentaId,
    String cuentaNombre,
    UUID instrumentoId,
    String instrumentoNombre,
    String instrumentoSimbolo,
    UUID tipoInstrumentoId,
    String tipoInstrumentoNombre,
    UUID claseActivoId,
    String claseActivoNombre,
    String cantidad,
    String costoUnitario,
    String costoTotal,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    String observaciones,
    boolean activo,
    LocalDate fechaApertura,
    Instant fechaCreacion,
    Instant fechaActualizacion
) {
}
