package com.patrimonio.inversiones.domain.model;

import java.math.BigDecimal;

/**
 * Base de las estrategias que incrementan cantidad y costo (APERTURA y
 * COMPRA). La diferencia es que una compra exige cantidad positiva.
 */
abstract class EstrategiaIncremento implements EstrategiaMovimiento {

    private final boolean exigeCantidadPositiva;

    protected EstrategiaIncremento(boolean exigeCantidadPositiva) {
        this.exigeCantidadPositiva = exigeCantidadPositiva;
    }

    @Override
    public ResultadoCalculo calcular(ContextoCalculo contexto) {
        if (exigeCantidadPositiva) {
            CalculosMovimiento.requerirPositivo(
                contexto.cantidad(), "cantidad");
        } else {
            CalculosMovimiento.requerirNoNegativo(
                contexto.cantidad(), "cantidad");
        }
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
        BigDecimal comisiones = nuloCero(contexto.comisiones());
        BigDecimal impuestos = nuloCero(contexto.impuestos());
        CalculosMovimiento.requerirNoNegativo(comisiones, "comisiones");
        CalculosMovimiento.requerirNoNegativo(impuestos, "impuestos");
        CalculosMovimiento.validarEscala(
            comisiones, CalculosMovimiento.ESCALA_IMPORTE, "comisiones");
        CalculosMovimiento.validarEscala(
            impuestos, CalculosMovimiento.ESCALA_IMPORTE, "impuestos");

        BigDecimal bruto = CalculosMovimiento.importe(
            contexto.cantidad(), contexto.precioUnitario());
        BigDecimal costoIncremento = bruto
            .add(comisiones)
            .add(impuestos)
            .setScale(CalculosMovimiento.ESCALA_IMPORTE, CalculosMovimiento.REDONDEO);
        BigDecimal cantidadResultante = contexto.cantidadActual()
            .add(contexto.cantidad());
        BigDecimal costoResultante = contexto.costoActual()
            .add(costoIncremento);

        return new ResultadoCalculo(
            bruto,
            costoIncremento,
            costoIncremento,
            null,
            contexto.cantidad(),
            costoIncremento,
            cantidadResultante,
            costoResultante);
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
