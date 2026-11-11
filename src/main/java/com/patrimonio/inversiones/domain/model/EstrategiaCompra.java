package com.patrimonio.inversiones.domain.model;

/**
 * Compra: incrementa la cantidad y el costo de adquisición con los gastos
 * atribuibles a la operación.
 */
public final class EstrategiaCompra extends EstrategiaIncremento {

    public EstrategiaCompra() {
        super(true);
    }

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.COMPRA;
    }
}
