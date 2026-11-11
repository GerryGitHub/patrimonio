package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Escalas y operaciones comunes del motor de movimientos.
 *
 * <p>La cantidad y los importes se conservan con 10 decimales; el precio
 * unitario con 8. Las multiplicaciones y divisiones se redondean una sola vez
 * al final con {@link RoundingMode#HALF_UP}. El costo total es la magnitud
 * autoritativa; el costo unitario es una vista derivada.</p>
 */
public final class CalculosMovimiento {

    public static final int ESCALA_CANTIDAD = 10;
    public static final int ESCALA_PRECIO = 8;
    public static final int ESCALA_IMPORTE = 10;
    public static final int ESCALA_COSTO_UNITARIO = 8;
    public static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private CalculosMovimiento() {
    }

    public static BigDecimal importe(BigDecimal cantidad, BigDecimal precio) {
        return cantidad.multiply(precio).setScale(ESCALA_IMPORTE, REDONDEO);
    }

    /**
     * Costo de la cantidad vendida por promedio ponderado.
     *
     * <p>Cuando se vende toda la tenencia se devuelve el costo total exacto
     * para evitar un residuo por redondeo.</p>
     */
    public static BigDecimal costoPromedioPonderado(
        BigDecimal costoActual,
        BigDecimal cantidad,
        BigDecimal cantidadActual
    ) {
        if (cantidadActual.signum() == 0) {
            throw new MovimientoInvalidoException(
                "No hay cantidad disponible para calcular el costo de la venta.");
        }
        if (cantidad.compareTo(cantidadActual) == 0) {
            return costoActual.setScale(ESCALA_IMPORTE, REDONDEO);
        }
        return costoActual
            .multiply(cantidad)
            .divide(cantidadActual, ESCALA_IMPORTE, REDONDEO);
    }

    public static BigDecimal costoUnitario(
        BigDecimal costoTotal,
        BigDecimal cantidad
    ) {
        if (cantidad.signum() == 0) {
            return BigDecimal.ZERO.setScale(ESCALA_COSTO_UNITARIO, REDONDEO);
        }
        return costoTotal.divide(
            cantidad, ESCALA_COSTO_UNITARIO, REDONDEO);
    }

    public static void validarEscala(
        BigDecimal valor,
        int escalaMaxima,
        String campo
    ) {
        if (valor == null) {
            return;
        }
        if (valor.stripTrailingZeros().scale() > escalaMaxima) {
            throw new MovimientoInvalidoException(
                "El campo '" + campo + "' admite como máximo "
                    + escalaMaxima + " decimales.");
        }
    }

    public static void requerirNoNegativo(BigDecimal valor, String campo) {
        if (valor == null) {
            throw new MovimientoInvalidoException(
                "El campo '" + campo + "' es obligatorio.");
        }
        if (valor.signum() < 0) {
            throw new MovimientoInvalidoException(
                "El campo '" + campo + "' no puede ser negativo.");
        }
    }

    public static void requerirPositivo(BigDecimal valor, String campo) {
        requerirNoNegativo(valor, campo);
        if (valor.signum() == 0) {
            throw new MovimientoInvalidoException(
                "El campo '" + campo + "' debe ser mayor que cero.");
        }
    }
}
