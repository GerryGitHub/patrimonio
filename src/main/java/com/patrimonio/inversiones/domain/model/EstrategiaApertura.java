package com.patrimonio.inversiones.domain.model;

/**
 * Reconocimiento de una tenencia inicial. Admite cantidad cero para conservar
 * posiciones migradas sin tenencia.
 */
public final class EstrategiaApertura extends EstrategiaIncremento {

    public EstrategiaApertura() {
        super(false);
    }

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.APERTURA;
    }
}
