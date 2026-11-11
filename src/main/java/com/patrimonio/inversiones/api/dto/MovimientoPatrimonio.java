package com.patrimonio.inversiones.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Movimiento confirmado publicado para la consolidación patrimonial.
 *
 * <p>Los deltas {@code efectoCantidad}, {@code efectoCosto} y el
 * {@code resultadoRealizado} son los valores que el motor de movimientos aplicó
 * a la posición. Las reversiones conservan el signo contrario al movimiento
 * original, de modo que sumar el historial completo no duplica una operación
 * revertida. El campo {@code tipo} es el código estable del catálogo
 * extensible de tipos de movimiento.</p>
 */
public record MovimientoPatrimonio(
    UUID id,
    UUID portafolioId,
    UUID cuentaId,
    UUID posicionId,
    UUID instrumentoId,
    UUID claseActivoId,
    String claseActivoNombre,
    UUID tipoInstrumentoId,
    String tipoInstrumentoNombre,
    String tipo,
    boolean reversion,
    LocalDate fechaOperacion,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal importeBruto,
    BigDecimal comisiones,
    BigDecimal impuestos,
    BigDecimal importeNeto,
    BigDecimal costoAplicado,
    BigDecimal resultadoRealizado,
    BigDecimal efectoCantidad,
    BigDecimal efectoCosto,
    UUID movimientoOrigenId,
    Instant fechaCreacion
) {
}
