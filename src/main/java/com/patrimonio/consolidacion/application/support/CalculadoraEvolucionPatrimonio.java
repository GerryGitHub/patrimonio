package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.port.out.ValuacionPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DesgloseClaseView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.PuntoEvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SerieEvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SerieMonedaView;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import com.patrimonio.consolidacion.domain.model.GranularidadEvolucion;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Cálculo de la evolución histórica patrimonial.
 *
 * <p>Las series se construyen con movimientos confirmados y no reconstruyen
 * valor de mercado con costos. Los movimientos anteriores al inicio solicitado
 * forman el saldo de apertura; no se inventan fechas anteriores al primer
 * movimiento.</p>
 */
@Component
public class CalculadoraEvolucionPatrimonio {

    private final MonedasPatrimonio monedas;
    private final ValuacionPort valuaciones;

    public CalculadoraEvolucionPatrimonio(
        MonedasPatrimonio monedas,
        ValuacionPort valuaciones
    ) {
        this.monedas = monedas;
        this.valuaciones = valuaciones;
    }

    public EvolucionView calcular(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query,
        LocalDate hasta
    ) {
        LocalDate hastaFinal = query.hasta() == null ? hasta : query.hasta();
        LocalDate desde = query.desde() == null
            ? query.periodo().desde(hastaFinal)
            : query.desde();
        GranularidadEvolucion granularidad =
            GranularidadEvolucion.paraPeriodo(query.periodo());

        List<MovimientoPatrimonio> enRango = contexto.movimientos().stream()
            .filter(movimiento ->
                !movimiento.fechaOperacion().isAfter(hastaFinal))
            .toList();

        List<SerieEvolucionView> series = List.of(
            serie(
                "COSTO_ADQUISICION",
                "Costo de adquisición vigente",
                enRango,
                MovimientoPatrimonio::efectoCosto,
                desde,
                hastaFinal,
                granularidad,
                query.monedaCodigo()),
            serie(
                "RESULTADO_REALIZADO",
                "Resultado realizado acumulado",
                enRango,
                movimiento -> nuloCero(movimiento.resultadoRealizado()),
                desde,
                hastaFinal,
                granularidad,
                query.monedaCodigo()),
            noDisponibleSerie(
                "VALOR_MERCADO",
                "Valor de mercado",
                valuaciones.disponible()
                    ? "La fuente de valuación no cubre el período solicitado."
                    : "Sin cotizaciones ni snapshots históricos de mercado."),
            noDisponibleSerie(
                "PATRIMONIO_NETO",
                "Patrimonio neto",
                "Requiere valor de mercado, efectivo y pasivos."),
            noDisponibleSerie(
                "LIQUIDEZ",
                "Liquidez",
                "No hay saldos de efectivo históricos registrados."),
            noDisponibleSerie(
                "CAPITAL_APORTADO",
                "Capital aportado",
                "No se modelan aportes ni retiros externos."));

        List<DesgloseClaseView> desglose = desglosePorClase(
            enRango, desde, hastaFinal, granularidad, query.monedaCodigo());

        return new EvolucionView(
            query.periodo(),
            desde,
            hastaFinal,
            granularidad,
            series,
            desglose,
            List.of(
                "Las series de costo y resultado se construyen con "
                    + "movimientos confirmados; no se reconstruye valor de "
                    + "mercado con costos.",
                "No se muestran datos anteriores al primer movimiento "
                    + "registrado.",
                "Valor de mercado, patrimonio neto, liquidez y capital "
                    + "aportado no están disponibles."));
    }

    private SerieEvolucionView serie(
        String codigo,
        String etiqueta,
        List<MovimientoPatrimonio> movimientos,
        Function<MovimientoPatrimonio, BigDecimal> valor,
        LocalDate desde,
        LocalDate hasta,
        GranularidadEvolucion granularidad,
        String monedaCodigo
    ) {
        List<SerieMonedaView> porMoneda = seriePorMoneda(
            movimientos, valor, desde, hasta, granularidad, monedaCodigo);
        return new SerieEvolucionView(
            codigo,
            etiqueta,
            EstadoInformacion.DISPONIBLE,
            porMoneda.isEmpty()
                ? "No hay movimientos confirmados en el período."
                : null,
            porMoneda);
    }

    private List<SerieMonedaView> seriePorMoneda(
        List<MovimientoPatrimonio> movimientos,
        Function<MovimientoPatrimonio, BigDecimal> valor,
        LocalDate desde,
        LocalDate hasta,
        GranularidadEvolucion granularidad,
        String monedaCodigo
    ) {
        Map<UUID, List<MovimientoPatrimonio>> porMoneda =
            movimientos.stream()
                .filter(movimiento ->
                    monedas.coincide(monedaCodigo, movimiento.monedaCodigo()))
                .collect(Collectors.groupingBy(
                    MovimientoPatrimonio::monedaId,
                    LinkedHashMap::new,
                    Collectors.toList()));

        List<SerieMonedaView> series = new ArrayList<>();
        for (List<MovimientoPatrimonio> lista : porMoneda.values()) {
            lista.sort(Comparator
                .comparing(MovimientoPatrimonio::fechaOperacion)
                .thenComparing(MovimientoPatrimonio::fechaCreacion));
            MovimientoPatrimonio primero = lista.get(0);
            LocalDate inicio = desde == null
                ? primero.fechaOperacion()
                : max(desde, primero.fechaOperacion());
            if (inicio.isAfter(hasta)) {
                continue;
            }
            BigDecimal acumulado = BigDecimal.ZERO;
            int indice = 0;
            for (MovimientoPatrimonio movimiento : lista) {
                if (movimiento.fechaOperacion().isBefore(inicio)) {
                    acumulado = acumulado.add(
                        nuloCero(valor.apply(movimiento)));
                    indice++;
                } else {
                    break;
                }
            }
            List<LocalDate> fechas = CalculadoraEvolucion.fechas(
                granularidad, inicio, hasta);
            List<PuntoEvolucionView> puntos = new ArrayList<>();
            for (LocalDate fecha : fechas) {
                while (indice < lista.size()
                    && !lista.get(indice).fechaOperacion().isAfter(fecha)) {
                    acumulado = acumulado.add(
                        nuloCero(valor.apply(lista.get(indice))));
                    indice++;
                }
                puntos.add(new PuntoEvolucionView(fecha, acumulado));
            }
            if (!puntos.isEmpty()) {
                series.add(new SerieMonedaView(
                    monedas.deMovimiento(primero), puntos));
            }
        }
        series.sort(Comparator.comparing(serie -> serie.moneda().codigo()));
        return series;
    }

    private List<DesgloseClaseView> desglosePorClase(
        List<MovimientoPatrimonio> movimientos,
        LocalDate desde,
        LocalDate hasta,
        GranularidadEvolucion granularidad,
        String monedaCodigo
    ) {
        Map<UUID, ClaseAcumulada> porClase = new LinkedHashMap<>();
        for (MovimientoPatrimonio movimiento : movimientos) {
            porClase.computeIfAbsent(
                movimiento.claseActivoId(),
                id -> new ClaseAcumulada(
                    movimiento.claseActivoId(),
                    movimiento.claseActivoNombre()))
                .movimientos.add(movimiento);
        }
        return porClase.values().stream()
            .map(clase -> new DesgloseClaseView(
                clase.id,
                clase.nombre,
                seriePorMoneda(
                    clase.movimientos,
                    MovimientoPatrimonio::efectoCosto,
                    desde,
                    hasta,
                    granularidad,
                    monedaCodigo)))
            .filter(desglose -> !desglose.porMoneda().isEmpty())
            .sorted(Comparator.comparing(DesgloseClaseView::claseActivoNombre))
            .toList();
    }

    private static SerieEvolucionView noDisponibleSerie(
        String codigo,
        String etiqueta,
        String motivo
    ) {
        return new SerieEvolucionView(
            codigo,
            etiqueta,
            EstadoInformacion.NO_DISPONIBLE,
            motivo,
            List.of());
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static LocalDate max(LocalDate primero, LocalDate segundo) {
        return primero.isAfter(segundo) ? primero : segundo;
    }

    private static final class ClaseAcumulada {

        private final UUID id;
        private final String nombre;
        private final List<MovimientoPatrimonio> movimientos =
            new ArrayList<>();

        private ClaseAcumulada(UUID id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }
}
