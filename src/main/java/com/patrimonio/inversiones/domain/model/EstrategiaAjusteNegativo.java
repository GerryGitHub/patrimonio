package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.math.BigDecimal;

/**
 * Ajuste negativo: corrección justificada que reduce cantidad y costo.
 *
 * <p>El costo total que se retira es explícito. Si se retira toda la tenencia,
 * el costo retirado debe coincidir con el costo total actual para no dejar un
 * residuo inconsistente.</p>
 */
public final class EstrategiaAjusteNegativo implements EstrategiaMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.AJUSTE_NEGATIVO;
    }

    @Override
    public ResultadoCalculo calcular(ContextoCalculo contexto) {
        CalculosMovimiento.requerirPositivo(contexto.cantidad(), "cantidad");
        CalculosMovimiento.validarEscala(
            contexto.cantidad(),
            CalculosMovimiento.ESCALA_CANTIDAD,
            "cantidad");
        if (contexto.cantidad().compareTo(contexto.cantidadActual()) > 0) {
            throw new MovimientoInvalidoException(
                "La cantidad del ajuste negativo supera la cantidad disponible.");
        }
        CalculosMovimiento.requerirNoNegativo(
            contexto.precioUnitario(), "precio unitario");
        CalculosMovimiento.validarEscala(
            contexto.precioUnitario(),
            CalculosMovimiento.ESCALA_PRECIO,
            "precio unitario");
        if (contexto.costoAjuste() == null) {
            throw new MovimientoInvalidoException(
                "El ajuste negativo debe indicar el costo total que se retira.");
        }
        CalculosMovimiento.requerirNoNegativo(
            contexto.costoAjuste(), "costo total ajustado");
        CalculosMovimiento.validarEscala(
            contexto.costoAjuste(),
            CalculosMovimiento.ESCALA_IMPORTE,
            "costo total ajustado");
        if (contexto.costoAjuste().compareTo(contexto.costoActual()) > 0) {
            throw new MovimientoInvalidoException(
                "El costo del ajuste negativo supera el costo total disponible.");
        }

        BigDecimal cantidadResultante = contexto.cantidadActual()
            .subtract(contexto.cantidad());
        BigDecimal costoResultante = contexto.costoActual()
            .subtract(contexto.costoAjuste());
        if (cantidadResultante.signum() == 0) {
            if (contexto.costoAjuste().compareTo(contexto.costoActual()) != 0) {
                throw new MovimientoInvalidoException(
                    "Si el ajuste deja la posición en cero, debe retirar todo el costo.");
            }
            costoResultante = BigDecimal.ZERO;
        }

        BigDecimal bruto = CalculosMovimiento.importe(
            contexto.cantidad(), contexto.precioUnitario());

        return new ResultadoCalculo(
            bruto,
            contexto.costoAjuste(),
            contexto.costoAjuste(),
            null,
            contexto.cantidad().negate(),
            contexto.costoAjuste().negate(),
            cantidadResultante,
            costoResultante);
    }
}
