package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.math.BigDecimal;

/**
 * Ajuste positivo: corrección justificada que aumenta cantidad y costo.
 *
 * <p>El costo total ajustado es explícito. No se deduce del precio para no
 * ocultar el componente de costo de la corrección.</p>
 */
public final class EstrategiaAjustePositivo implements EstrategiaMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.AJUSTE_POSITIVO;
    }

    @Override
    public ResultadoCalculo calcular(ContextoCalculo contexto) {
        CalculosMovimiento.requerirPositivo(contexto.cantidad(), "cantidad");
        CalculosMovimiento.validarEscala(
            contexto.cantidad(),
            CalculosMovimiento.ESCALA_CANTIDAD,
            "cantidad");
        CalculosMovimiento.requerirNoNegativo(
            contexto.precioUnitario(), "precio unitario");
        CalculosMovimiento.validarEscala(
            contexto.precioUnitario(),
            CalculosMovimiento.ESCALA_PRECIO,
            "precio unitario");
        if (contexto.costoAjuste() == null) {
            throw new MovimientoInvalidoException(
                "El ajuste positivo debe indicar el costo total que se agrega.");
        }
        CalculosMovimiento.requerirNoNegativo(
            contexto.costoAjuste(), "costo total ajustado");
        CalculosMovimiento.validarEscala(
            contexto.costoAjuste(),
            CalculosMovimiento.ESCALA_IMPORTE,
            "costo total ajustado");

        BigDecimal bruto = CalculosMovimiento.importe(
            contexto.cantidad(), contexto.precioUnitario());
        BigDecimal cantidadResultante = contexto.cantidadActual()
            .add(contexto.cantidad());
        BigDecimal costoResultante = contexto.costoActual()
            .add(contexto.costoAjuste());

        return new ResultadoCalculo(
            bruto,
            contexto.costoAjuste(),
            contexto.costoAjuste(),
            null,
            contexto.cantidad(),
            contexto.costoAjuste(),
            cantidadResultante,
            costoResultante);
    }
}
