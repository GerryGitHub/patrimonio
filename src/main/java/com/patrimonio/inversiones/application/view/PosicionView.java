package com.patrimonio.inversiones.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Vista de aplicación de una posición.
 *
 * <p>La cantidad y el costo son el estado materializado derivado de los
 * movimientos confirmados. El costo total es una magnitud de adquisición, no un
 * valor de mercado. El resultado realizado solo incluye ventas y reversiones.</p>
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
    BigDecimal resultadoRealizado,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    String observaciones,
    boolean activo,
    LocalDate fechaApertura,
    Instant fechaUltimoMovimiento,
    Instant fechaCreacion,
    Instant fechaActualizacion
) {
}
