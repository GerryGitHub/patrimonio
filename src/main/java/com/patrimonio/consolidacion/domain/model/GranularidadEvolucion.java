package com.patrimonio.consolidacion.domain.model;

/**
 * Granularidad de los puntos de una serie histórica.
 */
public enum GranularidadEvolucion {
    DIARIA,
    SEMANAL,
    MENSUAL;

    /**
     * Granularidad adecuada para el período solicitado.
     */
    public static GranularidadEvolucion paraPeriodo(PeriodoPatrimonial periodo) {
        return switch (periodo) {
            case UN_MES -> DIARIA;
            case TRES_MESES, SEIS_MESES -> SEMANAL;
            case UN_ANIO, TODO -> MENSUAL;
        };
    }
}
