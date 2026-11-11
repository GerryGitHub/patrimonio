package com.patrimonio.consolidacion.domain.model;

/**
 * Grado de completitud de un indicador patrimonial.
 *
 * <p>{@code DISPONIBLE} significa que el indicador se calcula con datos
 * registrados. {@code PARCIAL} indica que solo cubre una parte del patrimonio
 * (por ejemplo, inversiones a costo sin efectivo ni pasivos).
 * {@code NO_DISPONIBLE} significa que falta una fuente de datos completa; en
 * ese caso nunca se devuelve cero como si fuera un valor real.</p>
 */
public enum EstadoInformacion {
    DISPONIBLE,
    PARCIAL,
    NO_DISPONIBLE
}
