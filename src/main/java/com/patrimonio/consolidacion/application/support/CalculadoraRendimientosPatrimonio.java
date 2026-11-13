package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.RendimientosView;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cálculo de rendimientos y costos de operación.
 *
 * <p>El resultado realizado ya descuenta comisiones e impuestos de venta. Las
 * comisiones e impuestos mostrados corresponden a operaciones vigentes y no se
 * restan dos veces.</p>
 */
@Component
public class CalculadoraRendimientosPatrimonio {

    private final MovimientosPatrimonio movimientos;
    private final MonedasPatrimonio monedas;
    private final IndicadoresPatrimonio indicadores;

    public CalculadoraRendimientosPatrimonio(
        MovimientosPatrimonio movimientos,
        MonedasPatrimonio monedas,
        IndicadoresPatrimonio indicadores
    ) {
        this.movimientos = movimientos;
        this.monedas = monedas;
        this.indicadores = indicadores;
    }

    public RendimientosView calcular(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query,
        LocalDate hasta
    ) {
        LocalDate hastaFinal = query.hasta() == null ? hasta : query.hasta();
        LocalDate desde = query.desde() == null
            ? query.periodo().desde(hastaFinal)
            : query.desde();

        List<MovimientoPatrimonio> enRango =
            movimientos.efectivos(contexto.movimientos())
                .stream()
                .filter(movimiento ->
                    !movimiento.fechaOperacion().isAfter(hastaFinal))
                .filter(movimiento -> desde == null
                    || !movimiento.fechaOperacion().isBefore(desde))
                .toList();

        TotalesMoneda resultados = new TotalesMoneda();
        TotalesMoneda comisiones = new TotalesMoneda();
        TotalesMoneda impuestos = new TotalesMoneda();
        for (MovimientoPatrimonio movimiento : enRango) {
            if (!monedas.coincide(query, movimiento.monedaCodigo())) {
                continue;
            }
            resultados.sumar(
                movimiento.monedaId(),
                monedas.deMovimiento(movimiento),
                nuloCero(movimiento.resultadoRealizado()));
            comisiones.sumar(
                movimiento.monedaId(),
                monedas.deMovimiento(movimiento),
                nuloCero(movimiento.comisiones()));
            impuestos.sumar(
                movimiento.monedaId(),
                monedas.deMovimiento(movimiento),
                nuloCero(movimiento.impuestos()));
        }

        return new RendimientosView(
            desde,
            hastaFinal,
            indicadores.disponible(
                "RESULTADO_REALIZADO",
                "Resultado realizado",
                resultados,
                "Resultado neto de comisiones e impuestos de venta."),
            indicadores.disponible(
                "COMISIONES",
                "Comisiones pagadas",
                comisiones,
                "Comisiones de operaciones vigentes; no se restan dos veces "
                    + "porque el resultado realizado ya las descuenta."),
            indicadores.disponible(
                "IMPUESTOS",
                "Impuestos y retenciones",
                impuestos,
                "Impuestos de operaciones vigentes; no se restan dos veces "
                    + "porque el resultado realizado ya los descuenta."),
            indicadores.noDisponible(
                "INTERESES",
                "Intereses recibidos",
                "No existen contratos de inversión ni pagos registrados."),
            indicadores.noDisponible(
                "DIVIDENDOS",
                "Dividendos recibidos",
                "No existen eventos de dividendos registrados."),
            indicadores.noDisponible(
                "RESULTADO_NO_REALIZADO",
                "Ganancias no realizadas",
                "Requiere valuación de mercado confiable."),
            indicadores.noDisponible(
                "RENDIMIENTO_PORCENTUAL",
                "Rendimiento porcentual",
                "Requiere metodología TWR o XIRR y flujos de efectivo "
                    + "completos."),
            List.of("TWR", "XIRR"),
            "El resultado realizado ya descuenta comisiones e impuestos de "
                + "venta. Las comisiones e impuestos mostrados corresponden a "
                + "operaciones vigentes y no se restan dos veces.");
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
