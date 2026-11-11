package com.patrimonio.inversiones.domain.model;

/**
 * Estrategia de cálculo de un tipo de movimiento.
 *
 * <p>El motor selecciona la estrategia por tipo. Esto permite incorporar FIFO
 * en el futuro sin reescribir el agregado de posición ni el caso de uso.</p>
 */
public interface EstrategiaMovimiento {

    TipoMovimiento tipo();

    ResultadoCalculo calcular(ContextoCalculo contexto);
}
