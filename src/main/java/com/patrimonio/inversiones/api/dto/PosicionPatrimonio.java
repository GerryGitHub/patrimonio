package com.patrimonio.inversiones.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Posición publicada para la consolidación patrimonial.
 *
 * <p>Es un contrato de solo lectura que no expone la entidad JPA. El costo es
 * el costo de adquisición vigente de la posición y no representa valor de
 * mercado ni efectivo disponible. La clase y el tipo de instrumento se
 * resuelven desde los catálogos extensibles del módulo {@code inversiones}.</p>
 */
public record PosicionPatrimonio(
    UUID id,
    UUID cuentaId,
    UUID instrumentoId,
    String instrumentoNombre,
    String instrumentoSimbolo,
    UUID claseActivoId,
    String claseActivoNombre,
    UUID tipoInstrumentoId,
    String tipoInstrumentoNombre,
    BigDecimal cantidad,
    BigDecimal costoTotal,
    BigDecimal resultadoRealizado,
    UUID monedaId,
    String monedaCodigo,
    String monedaNombre,
    String monedaSimbolo,
    int monedaDecimales,
    boolean activo,
    LocalDate fechaApertura,
    Instant fechaUltimoMovimiento
) {
}
