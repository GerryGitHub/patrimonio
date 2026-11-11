package com.patrimonio.consolidacion.application.query;

import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.consolidacion.domain.model.PeriodoPatrimonial;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta de información patrimonial.
 *
 * @param usuarioId usuario autenticado que autoriza la consulta
 * @param portafolioId portafolio concreto o {@code null} para todos los
 *     portafolios autorizados
 * @param monedaCodigo filtro de presentación por código de moneda; no convierte
 *     importes
 * @param periodo período de las series y flujos
 * @param dimension dimensión de la distribución
 * @param desde inicio explícito para rendimientos; puede ser {@code null}
 * @param hasta fin explícito para rendimientos; puede ser {@code null}
 */
public record ConsultarPatrimonioQuery(
    UUID usuarioId,
    UUID portafolioId,
    String monedaCodigo,
    PeriodoPatrimonial periodo,
    DimensionDistribucion dimension,
    LocalDate desde,
    LocalDate hasta
) {

    public ConsultarPatrimonioQuery {
        Objects.requireNonNull(usuarioId, "usuarioId");
        periodo = periodo == null ? PeriodoPatrimonial.TODO : periodo;
        dimension = dimension == null
            ? DimensionDistribucion.CLASE_ACTIVO
            : dimension;
        monedaCodigo = monedaCodigo == null || monedaCodigo.isBlank()
            ? null
            : monedaCodigo.strip().toUpperCase();
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                "La fecha inicial no puede ser posterior a la final.");
        }
    }

    public static ConsultarPatrimonioQuery centro(
        UUID usuarioId,
        UUID portafolioId,
        String monedaCodigo,
        PeriodoPatrimonial periodo
    ) {
        return new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            monedaCodigo,
            periodo,
            DimensionDistribucion.CLASE_ACTIVO,
            null,
            null);
    }
}
