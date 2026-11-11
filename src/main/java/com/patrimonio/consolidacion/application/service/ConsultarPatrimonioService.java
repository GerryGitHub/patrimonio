package com.patrimonio.consolidacion.application.service;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.consolidacion.application.port.in.ConsultarCentroPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarDistribucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarEventosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarEvolucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarLiquidezPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarRendimientosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarResumenPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonio;
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonioPort;
import com.patrimonio.consolidacion.application.port.out.ValuacionPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraEvolucion;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CentroView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CoberturaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CuentaLiquidezView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DesgloseClaseView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionMonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EfectivoCuentaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EfectivoInstitucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventoView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventosView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ImporteMonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.IndicadorView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.LiquidezView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MetaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.PortafolioView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.PuntoEvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.RendimientosView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ResumenView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SegmentoDistribucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SerieEvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SerieMonedaView;
import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import com.patrimonio.consolidacion.domain.model.GranularidadEvolucion;
import com.patrimonio.consolidacion.domain.model.PeriodoPatrimonial;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.cuentas.api.port.CuentaPatrimonioPort;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.inversiones.api.port.MovimientoPatrimonioPort;
import com.patrimonio.inversiones.api.port.PosicionPatrimonioPort;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consolidación patrimonial de solo lectura.
 *
 * <p>El módulo no accede a repositorios ni entidades JPA de otros módulos:
 * consume únicamente los contratos públicos de {@code acceso}, {@code cuentas}
 * e {@code inversiones}. Los importes se agrupan por moneda y nunca se suman
 * divisas distintas. Los indicadores que requieren datos inexistentes se
 * devuelven como {@code NO_DISPONIBLE} con un motivo, sin rellenarlos con
 * costos ni ceros ficticios.</p>
 */
@Service
public class ConsultarPatrimonioService implements
    ConsultarCentroPatrimonioUseCase,
    ConsultarResumenPatrimonioUseCase,
    ConsultarDistribucionPatrimonioUseCase,
    ConsultarEvolucionPatrimonioUseCase,
    ConsultarRendimientosPatrimonioUseCase,
    ConsultarLiquidezPatrimonioUseCase,
    ConsultarEventosPatrimonioUseCase {

    private static final int ESCALA_PORCENTAJE = 4;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private static final List<String> COBERTURA_INCLUYE = List.of(
        "INVERSIONES_A_COSTO",
        "RESULTADO_REALIZADO",
        "COMISIONES",
        "IMPUESTOS");

    private static final List<String> COBERTURA_EXCLUYE = List.of(
        "EFECTIVO",
        "ACTIVOS_FISICOS",
        "PASIVOS",
        "VALOR_MERCADO",
        "INTERESES",
        "DIVIDENDOS",
        "CAPITAL_APORTADO");

    private static final List<String> LIMITACIONES = List.of(
        "No hay valuación de mercado: el patrimonio neto y las ganancias no "
            + "realizadas no pueden calcularse.",
        "No hay saldos de efectivo registrados: la liquidez disponible no "
            + "puede calcularse.",
        "No hay pasivos registrados: el patrimonio neto no puede calcularse.",
        "No se modelan aportes ni retiros externos: el capital aportado "
            + "histórico no puede calcularse.",
        "El costo de adquisición no es valor de mercado ni efectivo "
            + "disponible.");

    private final AccesoPatrimonio acceso;
    private final PortafolioQueryPort portafolios;
    private final CuentaPatrimonioPort cuentas;
    private final PosicionPatrimonioPort posiciones;
    private final MovimientoPatrimonioPort movimientos;
    private final MonedaCatalogoPort monedas;
    private final ValuacionPort valuaciones;
    private final EventoPatrimonioPort eventosPort;

    public ConsultarPatrimonioService(
        AccesoPatrimonio acceso,
        PortafolioQueryPort portafolios,
        CuentaPatrimonioPort cuentas,
        PosicionPatrimonioPort posiciones,
        MovimientoPatrimonioPort movimientos,
        MonedaCatalogoPort monedas,
        ValuacionPort valuaciones,
        EventoPatrimonioPort eventos
    ) {
        this.acceso = acceso;
        this.portafolios = portafolios;
        this.cuentas = cuentas;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.monedas = monedas;
        this.valuaciones = valuaciones;
        this.eventosPort = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public CentroView centro(ConsultarPatrimonioQuery query) {
        Contexto contexto = cargar(query);
        LocalDate hasta = contexto.hasta();
        return new CentroView(
            meta(contexto, query),
            resumen(contexto, query),
            distribucion(contexto, query),
            evolucion(contexto, query, hasta),
            rendimientos(contexto, query, hasta),
            liquidez(contexto, query),
            eventos(contexto, query, hasta));
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenView resumen(ConsultarPatrimonioQuery query) {
        return resumen(cargar(query), query);
    }

    @Override
    @Transactional(readOnly = true)
    public DistribucionView distribucion(ConsultarPatrimonioQuery query) {
        return distribucion(cargar(query), query);
    }

    @Override
    @Transactional(readOnly = true)
    public EvolucionView evolucion(ConsultarPatrimonioQuery query) {
        Contexto contexto = cargar(query);
        return evolucion(contexto, query, contexto.hasta());
    }

    @Override
    @Transactional(readOnly = true)
    public RendimientosView rendimientos(ConsultarPatrimonioQuery query) {
        Contexto contexto = cargar(query);
        return rendimientos(contexto, query, contexto.hasta());
    }

    @Override
    @Transactional(readOnly = true)
    public LiquidezView liquidez(ConsultarPatrimonioQuery query) {
        return liquidez(cargar(query), query);
    }

    @Override
    @Transactional(readOnly = true)
    public EventosView eventos(ConsultarPatrimonioQuery query) {
        Contexto contexto = cargar(query);
        return eventos(contexto, query, contexto.hasta());
    }

    // ------------------------------------------------------------------
    // Carga y autorización
    // ------------------------------------------------------------------

    private Contexto cargar(ConsultarPatrimonioQuery query) {
        List<PortafolioResumen> autorizados = portafoliosDe(query);
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        List<PortafolioDatos> datos = new ArrayList<>();
        for (PortafolioResumen portafolio : autorizados) {
            List<CuentaPatrimonio> cuentasPortafolio =
                cuentas.listarPorPortafolio(portafolio.id());
            Set<UUID> cuentaIds = cuentasPortafolio.stream()
                .map(CuentaPatrimonio::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            List<PosicionPatrimonio> posicionesPortafolio = cuentaIds.isEmpty()
                ? List.of()
                : posiciones.listarPorCuentas(cuentaIds, true);
            List<MovimientoPatrimonio> movimientosPortafolio =
                movimientos.listarPorPortafolio(portafolio.id(), null, hoy);
            datos.add(new PortafolioDatos(
                portafolio,
                cuentasPortafolio,
                posicionesPortafolio,
                movimientosPortafolio));
        }
        return new Contexto(datos, hoy);
    }

    private List<PortafolioResumen> portafoliosDe(
        ConsultarPatrimonioQuery query
    ) {
        if (query.portafolioId() == null) {
            return acceso.listarAutorizados(query.usuarioId());
        }
        acceso.exigirLectura(query.usuarioId(), query.portafolioId());
        PortafolioResumen portafolio = portafolios
            .buscarPorId(query.portafolioId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el portafolio indicado."));
        return List.of(portafolio);
    }

    // ------------------------------------------------------------------
    // Meta
    // ------------------------------------------------------------------

    private MetaView meta(Contexto contexto, ConsultarPatrimonioQuery query) {
        List<PortafolioView> vistas = contexto.portafolios().stream()
            .map(dato -> new PortafolioView(
                dato.portafolio().id(),
                dato.portafolio().nombre(),
                dato.portafolio().monedaBase().codigo(),
                dato.portafolio().monedaBase().decimales()))
            .toList();
        EstadoInformacion estado = vistas.isEmpty()
            ? EstadoInformacion.NO_DISPONIBLE
            : EstadoInformacion.PARCIAL;
        return new MetaView(
            vistas,
            query.monedaCodigo(),
            query.periodo(),
            Instant.now(),
            estado,
            new CoberturaView(COBERTURA_INCLUYE, COBERTURA_EXCLUYE),
            LIMITACIONES);
    }

    // ------------------------------------------------------------------
    // Resumen
    // ------------------------------------------------------------------

    private ResumenView resumen(
        Contexto contexto,
        ConsultarPatrimonioQuery query
    ) {
        Map<UUID, TotalMoneda> costos = new LinkedHashMap<>();
        for (PosicionPatrimonio posicion : contexto.posiciones()) {
            if (!coincideMoneda(query, posicion.monedaCodigo())) {
                continue;
            }
            costos.computeIfAbsent(
                posicion.monedaId(),
                id -> new TotalMoneda(moneda(posicion)))
                .sumar(posicion.costoTotal());
        }

        Map<UUID, TotalMoneda> resultados = new LinkedHashMap<>();
        for (MovimientoPatrimonio movimiento : efectivos(contexto.movimientos())) {
            if (!coincideMoneda(query, movimiento.monedaCodigo())) {
                continue;
            }
            resultados.computeIfAbsent(
                movimiento.monedaId(),
                id -> new TotalMoneda(moneda(movimiento)))
                .sumar(nuloCero(movimiento.resultadoRealizado()));
        }

        return new ResumenView(
            noDisponible(
                "PATRIMONIO_NETO",
                "Patrimonio neto",
                "Requiere valor de mercado, efectivo y pasivos registrados."),
            parcial(
                "ACTIVOS_REGISTRADOS",
                "Activos registrados a costo de adquisición",
                costos,
                "Solo incluye inversiones registradas a costo; no incluye "
                    + "efectivo, activos físicos ni valor de mercado."),
            noDisponible(
                "PASIVOS",
                "Pasivos",
                "No hay pasivos registrados."),
            parcial(
                "CAPITAL_INVERTIDO",
                "Capital invertido a costo de adquisición",
                costos,
                "Costo vigente de las posiciones activas; no es capital "
                    + "aportado histórico ni efectivo disponible."),
            noDisponible(
                "CAPITAL_APORTADO",
                "Capital aportado histórico",
                "No se modelan aportes ni retiros externos de efectivo."),
            noDisponible(
                "LIQUIDEZ_DISPONIBLE",
                "Liquidez disponible",
                "No hay saldos de efectivo registrados por cuenta."),
            disponible(
                "RESULTADO_REALIZADO",
                "Resultado realizado acumulado",
                resultados,
                resultados.isEmpty()
                    ? "No hay movimientos confirmados que hayan generado "
                        + "resultado realizado."
                    : "Calculado desde movimientos confirmados; incluye "
                        + "posiciones cerradas y descuenta comisiones e "
                        + "impuestos de venta."),
            noDisponible(
                "RESULTADO_NO_REALIZADO",
                "Ganancias no realizadas",
                "Requiere valuación de mercado confiable."));
    }

    // ------------------------------------------------------------------
    // Distribución
    // ------------------------------------------------------------------

    private DistribucionView distribucion(
        Contexto contexto,
        ConsultarPatrimonioQuery query
    ) {
        Map<UUID, CuentaPatrimonio> cuentasPorId = cuentasPorId(contexto);
        Map<UUID, DistribucionMonedaAcumulada> porMoneda =
            new LinkedHashMap<>();

        for (PosicionPatrimonio posicion : contexto.posiciones()) {
            if (!coincideMoneda(query, posicion.monedaCodigo())) {
                continue;
            }
            ClaveSegmento clave = claveSegmento(
                query.dimension(), posicion, cuentasPorId);
            DistribucionMonedaAcumulada acumulada = porMoneda.computeIfAbsent(
                posicion.monedaId(),
                id -> new DistribucionMonedaAcumulada(moneda(posicion)));
            acumulada.agregar(
                clave,
                posicion.costoTotal(),
                posicion.instrumentoId());
        }

        List<DistribucionMonedaView> vistas = porMoneda.values().stream()
            .map(acumulada -> acumulada.vista(query.dimension()))
            .sorted(Comparator.comparing(
                vista -> vista.moneda().codigo()))
            .toList();

        List<String> limitaciones = vistas.isEmpty()
            ? List.of("No hay posiciones activas registradas para distribuir.")
            : List.of(
                "Los porcentajes se calculan dentro de cada moneda; no se "
                    + "suman monedas distintas.",
                "La distribución usa el costo de adquisición de las "
                    + "posiciones activas, no valor de mercado.");

        return new DistribucionView(query.dimension(), vistas, limitaciones);
    }

    private ClaveSegmento claveSegmento(
        DimensionDistribucion dimension,
        PosicionPatrimonio posicion,
        Map<UUID, CuentaPatrimonio> cuentasPorId
    ) {
        return switch (dimension) {
            case CLASE_ACTIVO -> new ClaveSegmento(
                posicion.claseActivoId().toString(),
                posicion.claseActivoNombre());
            case TIPO_INSTRUMENTO -> new ClaveSegmento(
                posicion.tipoInstrumentoId().toString(),
                posicion.tipoInstrumentoNombre());
            case INSTITUCION -> {
                CuentaPatrimonio cuenta = cuentaRequerida(
                    cuentasPorId, posicion.cuentaId());
                yield new ClaveSegmento(
                    cuenta.institucionId().toString(),
                    cuenta.institucionNombre());
            }
            case CUENTA -> {
                CuentaPatrimonio cuenta = cuentaRequerida(
                    cuentasPorId, posicion.cuentaId());
                yield new ClaveSegmento(cuenta.id().toString(), cuenta.nombre());
            }
            case MONEDA -> new ClaveSegmento(
                posicion.monedaId().toString(),
                posicion.monedaCodigo());
        };
    }

    // ------------------------------------------------------------------
    // Evolución
    // ------------------------------------------------------------------

    private EvolucionView evolucion(
        Contexto contexto,
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
            .filter(movimiento -> !movimiento.fechaOperacion().isAfter(hastaFinal))
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
                .filter(movimiento -> coincideMoneda(monedaCodigo, movimiento.monedaCodigo()))
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
                    acumulado = acumulado.add(nuloCero(valor.apply(movimiento)));
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
                series.add(new SerieMonedaView(moneda(primero), puntos));
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

    // ------------------------------------------------------------------
    // Rendimientos
    // ------------------------------------------------------------------

    private RendimientosView rendimientos(
        Contexto contexto,
        ConsultarPatrimonioQuery query,
        LocalDate hasta
    ) {
        LocalDate hastaFinal = query.hasta() == null ? hasta : query.hasta();
        LocalDate desde = query.desde() == null
            ? query.periodo().desde(hastaFinal)
            : query.desde();

        List<MovimientoPatrimonio> enRango = efectivos(contexto.movimientos())
            .stream()
            .filter(movimiento -> !movimiento.fechaOperacion().isAfter(hastaFinal))
            .filter(movimiento -> desde == null
                || !movimiento.fechaOperacion().isBefore(desde))
            .toList();

        Map<UUID, TotalMoneda> resultados = new LinkedHashMap<>();
        Map<UUID, TotalMoneda> comisiones = new LinkedHashMap<>();
        Map<UUID, TotalMoneda> impuestos = new LinkedHashMap<>();
        for (MovimientoPatrimonio movimiento : enRango) {
            if (!coincideMoneda(query, movimiento.monedaCodigo())) {
                continue;
            }
            TotalMoneda resultado = resultados.computeIfAbsent(
                movimiento.monedaId(),
                id -> new TotalMoneda(moneda(movimiento)));
            resultado.sumar(nuloCero(movimiento.resultadoRealizado()));
            comisiones.computeIfAbsent(
                movimiento.monedaId(),
                id -> new TotalMoneda(moneda(movimiento)))
                .sumar(nuloCero(movimiento.comisiones()));
            impuestos.computeIfAbsent(
                movimiento.monedaId(),
                id -> new TotalMoneda(moneda(movimiento)))
                .sumar(nuloCero(movimiento.impuestos()));
        }

        return new RendimientosView(
            desde,
            hastaFinal,
            disponible(
                "RESULTADO_REALIZADO",
                "Resultado realizado",
                resultados,
                "Resultado neto de comisiones e impuestos de venta."),
            disponible(
                "COMISIONES",
                "Comisiones pagadas",
                comisiones,
                "Comisiones de operaciones vigentes; no se restan dos veces "
                    + "porque el resultado realizado ya las descuenta."),
            disponible(
                "IMPUESTOS",
                "Impuestos y retenciones",
                impuestos,
                "Impuestos de operaciones vigentes; no se restan dos veces "
                    + "porque el resultado realizado ya los descuenta."),
            noDisponible(
                "INTERESES",
                "Intereses recibidos",
                "No existen contratos de inversión ni pagos registrados."),
            noDisponible(
                "DIVIDENDOS",
                "Dividendos recibidos",
                "No existen eventos de dividendos registrados."),
            noDisponible(
                "RESULTADO_NO_REALIZADO",
                "Ganancias no realizadas",
                "Requiere valuación de mercado confiable."),
            noDisponible(
                "RENDIMIENTO_PORCENTUAL",
                "Rendimiento porcentual",
                "Requiere metodología TWR o XIRR y flujos de efectivo "
                    + "completos."),
            List.of("TWR", "XIRR"),
            "El resultado realizado ya descuenta comisiones e impuestos de "
                + "venta. Las comisiones e impuestos mostrados corresponden a "
                + "operaciones vigentes y no se restan dos veces.");
    }

    // ------------------------------------------------------------------
    // Liquidez
    // ------------------------------------------------------------------

    private LiquidezView liquidez(
        Contexto contexto,
        ConsultarPatrimonioQuery query
    ) {
        List<CuentaLiquidezView> cuentasLiquidez = new ArrayList<>();
        for (CuentaPatrimonio cuenta : contexto.cuentas()) {
            MonedaView monedaCuenta = monedaDeCuenta(cuenta.monedaId());
            if (!coincideMoneda(query, monedaCuenta.codigo())) {
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

        Map<UUID, TotalMoneda> comprometido = new LinkedHashMap<>();
        for (PosicionPatrimonio posicion : contexto.posiciones()) {
            if (!coincideMoneda(query, posicion.monedaCodigo())) {
                continue;
            }
            comprometido.computeIfAbsent(
                posicion.monedaId(),
                id -> new TotalMoneda(moneda(posicion)))
                .sumar(posicion.costoTotal());
        }

        EventosView vencimientos = eventos(
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
            noDisponible(
                "DISPONIBILIDAD_INMEDIATA",
                "Disponibilidad inmediata",
                "No hay saldos de efectivo registrados."),
            parcial(
                "CAPITAL_COMPROMETIDO",
                "Costo de adquisición de inversiones activas",
                comprometido,
                "Es costo comprometido en inversiones, no efectivo "
                    + "disponible."),
            vencimientos);
    }

    // ------------------------------------------------------------------
    // Eventos
    // ------------------------------------------------------------------

    private EventosView eventos(
        Contexto contexto,
        ConsultarPatrimonioQuery query,
        LocalDate hasta
    ) {
        if (!eventosPort.disponible()) {
            return new EventosView(
                EstadoInformacion.NO_DISPONIBLE,
                "No existen contratos de inversión, pagos programados ni "
                    + "dividendos registrados.",
                List.of());
        }

        LocalDate desde = query.desde();
        LocalDate hastaFinal = query.hasta() == null ? hasta : query.hasta();
        List<EventoView> vistas = new ArrayList<>();
        for (PortafolioDatos dato : contexto.portafolios()) {
            for (EventoPatrimonio evento : eventosPort.listar(
                dato.portafolio().id(), desde, hastaFinal)) {
                MonedaView monedaEvento = evento.monedaId() == null
                    ? null
                    : monedaDeCuenta(evento.monedaId());
                if (monedaEvento != null
                    && !coincideMoneda(query, monedaEvento.codigo())) {
                    continue;
                }
                vistas.add(new EventoView(
                    evento.id(),
                    evento.tipo(),
                    evento.fecha(),
                    evento.descripcion(),
                    evento.importe(),
                    monedaEvento,
                    evento.origen(),
                    evento.posicionId(),
                    evento.contratoId()));
            }
        }
        vistas.sort(Comparator.comparing(EventoView::fecha));
        return new EventosView(
            EstadoInformacion.DISPONIBLE,
            vistas.isEmpty()
                ? "No hay eventos programados en el período."
                : null,
            vistas);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private static List<MovimientoPatrimonio> efectivos(
        List<MovimientoPatrimonio> movimientos
    ) {
        Set<UUID> revertidos = movimientos.stream()
            .map(MovimientoPatrimonio::movimientoOrigenId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return movimientos.stream()
            .filter(movimiento -> !movimiento.reversion())
            .filter(movimiento -> !revertidos.contains(movimiento.id()))
            .toList();
    }

    private Map<UUID, CuentaPatrimonio> cuentasPorId(Contexto contexto) {
        Map<UUID, CuentaPatrimonio> resultado = new HashMap<>();
        for (CuentaPatrimonio cuenta : contexto.cuentas()) {
            resultado.put(cuenta.id(), cuenta);
        }
        return resultado;
    }

    private static CuentaPatrimonio cuentaRequerida(
        Map<UUID, CuentaPatrimonio> cuentas,
        UUID cuentaId
    ) {
        CuentaPatrimonio cuenta = cuentas.get(cuentaId);
        if (cuenta == null) {
            throw new DatosPersistidosInvalidos(
                "La posición referencia una cuenta inexistente: " + cuentaId);
        }
        return cuenta;
    }

    private static boolean coincideMoneda(
        ConsultarPatrimonioQuery query,
        String codigo
    ) {
        return coincideMoneda(query.monedaCodigo(), codigo);
    }

    private static boolean coincideMoneda(String filtro, String codigo) {
        return filtro == null
            || (codigo != null && filtro.equalsIgnoreCase(codigo));
    }

    private MonedaView monedaDeCuenta(UUID monedaId) {
        MonedaResumen moneda = monedas.buscarPorId(monedaId)
            .orElseThrow(() -> new DatosPersistidosInvalidos(
                "La cuenta referencia una moneda inexistente: " + monedaId));
        return new MonedaView(
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales());
    }

    private static MonedaView moneda(PosicionPatrimonio posicion) {
        return new MonedaView(
            posicion.monedaId(),
            posicion.monedaCodigo(),
            posicion.monedaNombre(),
            posicion.monedaSimbolo(),
            posicion.monedaDecimales());
    }

    private static MonedaView moneda(MovimientoPatrimonio movimiento) {
        return new MonedaView(
            movimiento.monedaId(),
            movimiento.monedaCodigo(),
            movimiento.monedaNombre(),
            movimiento.monedaSimbolo(),
            movimiento.monedaDecimales());
    }

    private static BigDecimal nuloCero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private static LocalDate max(LocalDate primero, LocalDate segundo) {
        return primero.isAfter(segundo) ? primero : segundo;
    }

    private static IndicadorView disponible(
        String codigo,
        String etiqueta,
        Map<UUID, TotalMoneda> totales,
        String motivo
    ) {
        return new IndicadorView(
            codigo,
            etiqueta,
            EstadoInformacion.DISPONIBLE,
            totales.isEmpty() ? motivo : null,
            importes(totales));
    }

    private static IndicadorView parcial(
        String codigo,
        String etiqueta,
        Map<UUID, TotalMoneda> totales,
        String motivo
    ) {
        return new IndicadorView(
            codigo,
            etiqueta,
            EstadoInformacion.PARCIAL,
            motivo,
            importes(totales));
    }

    private static IndicadorView noDisponible(
        String codigo,
        String etiqueta,
        String motivo
    ) {
        return new IndicadorView(
            codigo,
            etiqueta,
            EstadoInformacion.NO_DISPONIBLE,
            motivo,
            List.of());
    }

    private static List<ImporteMonedaView> importes(
        Map<UUID, TotalMoneda> totales
    ) {
        return totales.values().stream()
            .sorted(Comparator.comparing(total -> total.moneda.codigo()))
            .map(total -> ImporteMonedaView.desde(total.moneda, total.importe))
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

    // ------------------------------------------------------------------
    // Tipos internos
    // ------------------------------------------------------------------

    private record Contexto(
        List<PortafolioDatos> portafolios,
        LocalDate hasta
    ) {
        List<PosicionPatrimonio> posiciones() {
            return portafolios.stream()
                .flatMap(dato -> dato.posiciones().stream())
                .toList();
        }

        List<MovimientoPatrimonio> movimientos() {
            return portafolios.stream()
                .flatMap(dato -> dato.movimientos().stream())
                .toList();
        }

        List<CuentaPatrimonio> cuentas() {
            return portafolios.stream()
                .flatMap(dato -> dato.cuentas().stream())
                .toList();
        }
    }

    private record PortafolioDatos(
        PortafolioResumen portafolio,
        List<CuentaPatrimonio> cuentas,
        List<PosicionPatrimonio> posiciones,
        List<MovimientoPatrimonio> movimientos
    ) {
    }

    private record ClaveSegmento(String clave, String nombre) {
    }

    private static final class TotalMoneda {

        private final MonedaView moneda;
        private BigDecimal importe = BigDecimal.ZERO;

        private TotalMoneda(MonedaView moneda) {
            this.moneda = moneda;
        }

        private void sumar(BigDecimal valor) {
            importe = importe.add(valor);
        }
    }

    private static final class DistribucionMonedaAcumulada {

        private final MonedaView moneda;
        private final Map<String, SegmentoAcumulado> segmentos =
            new LinkedHashMap<>();
        private BigDecimal total = BigDecimal.ZERO;

        private DistribucionMonedaAcumulada(MonedaView moneda) {
            this.moneda = moneda;
        }

        private void agregar(
            ClaveSegmento clave,
            BigDecimal importe,
            UUID instrumentoId
        ) {
            SegmentoAcumulado segmento = segmentos.computeIfAbsent(
                clave.clave(),
                id -> new SegmentoAcumulado(clave.clave(), clave.nombre()));
            segmento.importe = segmento.importe.add(importe);
            segmento.posiciones++;
            segmento.instrumentos.add(instrumentoId);
            total = total.add(importe);
        }

        private DistribucionMonedaView vista(DimensionDistribucion dimension) {
            List<SegmentoDistribucionView> vistas = segmentos.values()
                .stream()
                .map(segmento -> new SegmentoDistribucionView(
                    segmento.clave,
                    segmento.nombre,
                    segmento.importe,
                    dimension == DimensionDistribucion.MONEDA
                        ? null
                        : porcentaje(segmento.importe, total),
                    segmento.posiciones,
                    segmento.instrumentos.size()))
                .sorted(Comparator
                    .comparing(SegmentoDistribucionView::importe)
                    .reversed()
                    .thenComparing(SegmentoDistribucionView::nombre))
                .toList();
            return new DistribucionMonedaView(moneda, total, vistas);
        }

        private static BigDecimal porcentaje(
            BigDecimal importe,
            BigDecimal total
        ) {
            if (total.signum() == 0) {
                return null;
            }
            return importe.multiply(CIEN)
                .divide(total, ESCALA_PORCENTAJE, REDONDEO);
        }
    }

    private static final class SegmentoAcumulado {

        private final String clave;
        private final String nombre;
        private final Set<UUID> instrumentos = new HashSet<>();
        private BigDecimal importe = BigDecimal.ZERO;
        private int posiciones;

        private SegmentoAcumulado(String clave, String nombre) {
            this.clave = clave;
            this.nombre = nombre;
        }
    }

    private static final class ClaseAcumulada {

        private final UUID id;
        private final String nombre;
        private final List<MovimientoPatrimonio> movimientos = new ArrayList<>();

        private ClaseAcumulada(UUID id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }
}
