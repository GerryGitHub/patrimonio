package com.patrimonio.inversiones.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de aplicación de una posición.
 *
 * <p>El costo total es el único importe derivado: {@code cantidad × costo
 * unitario}. No es valor de mercado ni incluye rendimiento.</p>
 */
public record PosicionView(
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
    BigDecimal cantidad,
    BigDecimal costoUnitario,
    BigDecimal costoTotal,
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
