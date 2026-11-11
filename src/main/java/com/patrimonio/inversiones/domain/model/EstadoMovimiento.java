package com.patrimonio.inversiones.domain.model;

/**
 * Estados de un movimiento. En esta entrega todo movimiento nace confirmado y
 * es inmutable; las correcciones se representan con una {@code REVERSION}.
 */
public enum EstadoMovimiento {
    CONFIRMADO
}
