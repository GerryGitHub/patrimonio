package com.patrimonio.consolidacion.domain.model;

import java.time.LocalDate;

/**
 * Período de consulta de la evolución y los rendimientos.
 *
 * <p>El período no altera los saldos actuales: solo limita las series
 * históricas y los flujos mostrados. {@code TODO} representa el historial
 * completo disponible y no inventa fechas anteriores al primer registro.</p>
 */
public enum PeriodoPatrimonial {

    UN_MES("1M", 1),
    TRES_MESES("3M", 3),
    SEIS_MESES("6M", 6),
    UN_ANIO("1A", 12),
    TODO("TODO", 0);

    private final String codigo;
    private final int meses;

    PeriodoPatrimonial(String codigo, int meses) {
        this.codigo = codigo;
        this.meses = meses;
    }

    public String codigo() {
        return codigo;
    }

    /**
     * Fecha de inicio inclusiva o {@code null} cuando el período es todo el
     * historial.
     */
    public LocalDate desde(LocalDate hasta) {
        return this == TODO ? null : hasta.minusMonths(meses);
    }

    public static PeriodoPatrimonial desdeCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return TODO;
        }
        String normalizado = codigo.strip().toUpperCase();
        for (PeriodoPatrimonial periodo : values()) {
            if (periodo.codigo.equals(normalizado)) {
                return periodo;
            }
        }
        throw new IllegalArgumentException(
            "Período patrimonial desconocido: " + codigo);
    }
}
