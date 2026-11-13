package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ResumenView;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Cálculo del resumen patrimonial.
 *
 * <p>El costo de adquisición se presenta como activo registrado y capital
 * invertido, nunca como capital aportado ni como efectivo. Cada moneda
 * conserva su propio total.</p>
 */
@Component
public class CalculadoraResumenPatrimonio {

    private final MovimientosPatrimonio movimientos;
    private final MonedasPatrimonio monedas;
    private final IndicadoresPatrimonio indicadores;

    public CalculadoraResumenPatrimonio(
        MovimientosPatrimonio movimientos,
        MonedasPatrimonio monedas,
        IndicadoresPatrimonio indicadores
    ) {
        this.movimientos = movimientos;
        this.monedas = monedas;
        this.indicadores = indicadores;
    }

    public ResumenView calcular(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query
    ) {
        TotalesMoneda costos = new TotalesMoneda();
        for (PosicionPatrimonio posicion : contexto.posiciones()) {
            if (!monedas.coincide(query, posicion.monedaCodigo())) {
                continue;
            }
            costos.sumar(
                posicion.monedaId(),
                monedas.dePosicion(posicion),
                posicion.costoTotal());
        }

        TotalesMoneda resultados = new TotalesMoneda();
        for (MovimientoPatrimonio movimiento
            : movimientos.efectivos(contexto.movimientos())) {
            if (!monedas.coincide(query, movimiento.monedaCodigo())) {
                continue;
            }
            resultados.sumar(
                movimiento.monedaId(),
                monedas.deMovimiento(movimiento),
                nuloCero(movimiento.resultadoRealizado()));
        }

        return new ResumenView(
            indicadores.noDisponible(
                "PATRIMONIO_NETO",
                "Patrimonio neto",
                "Requiere valor de mercado, efectivo y pasivos registrados."),
            indicadores.parcial(
                "ACTIVOS_REGISTRADOS",
                "Activos registrados a costo de adquisición",
                costos,
                "Solo incluye inversiones registradas a costo; no incluye "
                    + "efectivo, activos físicos ni valor de mercado."),
            indicadores.noDisponible(
                "PASIVOS",
                "Pasivos",
                "No hay pasivos registrados."),
            indicadores.parcial(
                "CAPITAL_INVERTIDO",
                "Capital invertido a costo de adquisición",
                costos,
                "Costo vigente de las posiciones activas; no es capital "
                    + "aportado histórico ni efectivo disponible."),
            indicadores.noDisponible(
                "CAPITAL_APORTADO",
                "Capital aportado histórico",
                "No se modelan aportes ni retiros externos de efectivo."),
            indicadores.noDisponible(
                "LIQUIDEZ_DISPONIBLE",
                "Liquidez disponible",
                "No hay saldos de efectivo registrados por cuenta."),
            indicadores.disponible(
                "RESULTADO_REALIZADO",
                "Resultado realizado acumulado",
                resultados,
                resultados.vacio()
                    ? "No hay movimientos confirmados que hayan generado "
                        + "resultado realizado."
                    : "Calculado desde movimientos confirmados; incluye "
                        + "posiciones cerradas y descuenta comisiones e "
                        + "impuestos de venta."),
            indicadores.noDisponible(
                "RESULTADO_NO_REALIZADO",
                "Ganancias no realizadas",
                "Requiere valuación de mercado confiable."));
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
