package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.math.BigDecimal;

/**
 * Venta: reduce la cantidad, consume costo por promedio ponderado y reconoce
 * el resultado realizado.
 */
public final class EstrategiaVenta implements EstrategiaMovimiento {

    @Override
    public TipoMovimiento tipo() {
        return TipoMovimiento.VENTA;
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
                "La cantidad vendida supera la cantidad disponible en la posición.");
        }
        CalculosMovimiento.requerirNoNegativo(
            contexto.precioUnitario(), "precio unitario");
        CalculosMovimiento.validarEscala(
            contexto.precioUnitario(),
            CalculosMovimiento.ESCALA_PRECIO,
            "precio unitario");

        BigDecimal comisiones = nuloCero(contexto.comisiones());
        BigDecimal impuestos = nuloCero(contexto.impuestos());
        CalculosMovimiento.requerirNoNegativo(comisiones, "comisiones");
        CalculosMovimiento.requerirNoNegativo(impuestos, "impuestos");

        BigDecimal bruto = CalculosMovimiento.importe(
            contexto.cantidad(), contexto.precioUnitario());
        BigDecimal neto = bruto
            .subtract(comisiones)
            .subtract(impuestos)
            .setScale(CalculosMovimiento.ESCALA_IMPORTE, CalculosMovimiento.REDONDEO);
        if (neto.signum() < 0) {
            throw new MovimientoInvalidoException(
                "Las comisiones e impuestos no pueden superar el importe bruto de la venta.");
        }

        BigDecimal costoAplicado = CalculosMovimiento.costoPromedioPonderado(
            contexto.costoActual(),
            contexto.cantidad(),
            contexto.cantidadActual());
        if (costoAplicado.compareTo(contexto.costoActual()) > 0) {
            costoAplicado = contexto.costoActual();
        }

        BigDecimal cantidadResultante = contexto.cantidadActual()
            .subtract(contexto.cantidad());
        BigDecimal costoResultante = contexto.costoActual()
            .subtract(costoAplicado);
        if (cantidadResultante.signum() == 0) {
            costoResultante = BigDecimal.ZERO;
        }

        BigDecimal resultado = neto.subtract(costoAplicado)
            .setScale(CalculosMovimiento.ESCALA_IMPORTE, CalculosMovimiento.REDONDEO);

        return new ResultadoCalculo(
            bruto,
            neto,
            costoAplicado,
            resultado,
            contexto.cantidad().negate(),
            costoAplicado.negate(),
            cantidadResultante,
            costoResultante);
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
