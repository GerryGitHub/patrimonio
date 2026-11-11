package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.math.BigDecimal;

/**
 * Reversión exacta de un movimiento confirmado.
 *
 * <p>Invierte los efectos almacenados del movimiento original. Si la posición
 * cambió después, la reversión solo se acepta cuando el estado resultante sigue
 * siendo no negativo; de lo contrario se rechaza para no corromper saldos.</p>
 */
public final class EstrategiaReversion implements EstrategiaMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.REVERSION;
    }

    @Override
    public ResultadoCalculo calcular(ContextoCalculo contexto) {
        if (contexto.efectoCantidadOrigen() == null
            || contexto.efectoCostoOrigen() == null) {
            throw new MovimientoInvalidoException(
                "La reversión requiere los efectos del movimiento original.");
        }

        BigDecimal efectoCantidad = contexto.efectoCantidadOrigen().negate();
        BigDecimal efectoCosto = contexto.efectoCostoOrigen().negate();
        BigDecimal cantidadResultante = contexto.cantidadActual()
            .add(efectoCantidad);
        BigDecimal costoResultante = contexto.costoActual()
            .add(efectoCosto);

        if (cantidadResultante.signum() < 0) {
            throw new MovimientoInvalidoException(
                "La reversión produciría una cantidad negativa en la posición.");
        }
        if (costoResultante.signum() < 0) {
            throw new MovimientoInvalidoException(
                "La reversión produciría un costo negativo en la posición.");
        }
        if (cantidadResultante.signum() == 0) {
            costoResultante = BigDecimal.ZERO;
        }

        BigDecimal resultado = contexto.resultadoRealizadoOrigen() == null
            ? null
            : contexto.resultadoRealizadoOrigen().negate();

        return new ResultadoCalculo(
            nuloCero(contexto.importeBrutoOrigen()),
            nuloCero(contexto.importeNetoOrigen()),
            nuloCero(contexto.costoAplicadoOrigen()),
            resultado,
            efectoCantidad,
            efectoCosto,
            cantidadResultante,
            costoResultante);
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
