package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CuentaLiquidezView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EfectivoCuentaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EfectivoInstitucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventosView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.LiquidezView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cálculo de liquidez y capital comprometido.
 *
 * <p>No hay saldos de efectivo registrados, por lo que la liquidez se presenta
 * como {@code NO_DISPONIBLE}. El capital comprometido es el costo de
 * adquisición de las inversiones activas, no efectivo disponible.</p>
 */
@Component
public class CalculadoraLiquidezPatrimonio {

    private final MonedasPatrimonio monedas;
    private final CalculadoraEventosPatrimonio eventos;
    private final IndicadoresPatrimonio indicadores;

    public CalculadoraLiquidezPatrimonio(
        MonedasPatrimonio monedas,
        CalculadoraEventosPatrimonio eventos,
        IndicadoresPatrimonio indicadores
    ) {
        this.monedas = monedas;
        this.eventos = eventos;
        this.indicadores = indicadores;
    }

    public LiquidezView calcular(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query
    ) {
        List<CuentaLiquidezView> cuentasLiquidez = new ArrayList<>();
        for (CuentaPatrimonio cuenta : contexto.cuentas()) {
            MonedaView monedaCuenta = monedas.deCuenta(cuenta.monedaId());
            if (!monedas.coincide(query, monedaCuenta.codigo())) {
                continue;
            }
            cuentasLiquidez.add(new CuentaLiquidezView(
                cuenta.id(),
                cuenta.nombre(),
                cuenta.institucionNombre(),
                monedaCuenta,
                cuenta.activo(),
                null));
        }

        TotalesMoneda comprometido = new TotalesMoneda();
        for (PosicionPatrimonio posicion : contexto.posiciones()) {
            if (!monedas.coincide(query, posicion.monedaCodigo())) {
                continue;
            }
            comprometido.sumar(
                posicion.monedaId(),
                monedas.dePosicion(posicion),
                posicion.costoTotal());
        }

        EventosView vencimientos = eventos.calcular(
            contexto,
            query,
            LocalDate.now(ZoneOffset.UTC));

        return new LiquidezView(
            EstadoInformacion.PARCIAL,
            new EfectivoCuentaView(
                EstadoInformacion.NO_DISPONIBLE,
                "Las cuentas no tienen saldo de efectivo registrado.",
                cuentasLiquidez),
            new EfectivoInstitucionView(
                EstadoInformacion.NO_DISPONIBLE,
                "No hay saldos de efectivo para agrupar por institución.",
                List.of()),
            indicadores.noDisponible(
                "DISPONIBILIDAD_INMEDIATA",
                "Disponibilidad inmediata",
                "No hay saldos de efectivo registrados."),
            indicadores.parcial(
                "CAPITAL_COMPROMETIDO",
                "Costo de adquisición de inversiones activas",
                comprometido,
                "Es costo comprometido en inversiones, no efectivo "
                    + "disponible."),
            vencimientos);
    }
}
