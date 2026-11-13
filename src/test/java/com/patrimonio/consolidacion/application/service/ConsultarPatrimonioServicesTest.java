package com.patrimonio.consolidacion.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonio;
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonioPort;
import com.patrimonio.consolidacion.application.port.out.ValuacionPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraDistribucionPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraEventosPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraEvolucionPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraLiquidezPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraRendimientosPatrimonio;
import com.patrimonio.consolidacion.application.support.CalculadoraResumenPatrimonio;
import com.patrimonio.consolidacion.application.support.CargadorContextoPatrimonial;
import com.patrimonio.consolidacion.application.support.IndicadoresPatrimonio;
import com.patrimonio.consolidacion.application.support.MetadatosPatrimonio;
import com.patrimonio.consolidacion.application.support.MonedasPatrimonio;
import com.patrimonio.consolidacion.application.support.MovimientosPatrimonio;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CentroView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EventosView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ImporteMonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.LiquidezView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.RendimientosView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ResumenView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SerieEvolucionView;
import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas de las reglas financieras de las consultas patrimoniales.
 *
 * <p>Cubren la separación entre costo y capital aportado, la ausencia de suma
 * entre monedas, el resultado realizado sin doble conteo de reversiones, la
 * evolución sin datos inventados, la autorización global por membresía y el
 * manejo de referencias persistidas inconsistentes.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConsultarPatrimonioServicesTest {

    @Mock
    private AccesoQueryPort accesoQueryPort;
    @Mock
    private PortafolioQueryPort portafolios;
    @Mock
    private CuentaPatrimonioPort cuentas;
    @Mock
    private PosicionPatrimonioPort posiciones;
    @Mock
    private MovimientoPatrimonioPort movimientos;
    @Mock
    private MonedaCatalogoPort monedas;
    @Mock
    private ValuacionPort valuaciones;
    @Mock
    private EventoPatrimonioPort eventosPort;

    private ConsultarCentroPatrimonioService centro;
    private ConsultarResumenPatrimonioService resumen;
    private ConsultarDistribucionPatrimonioService distribucion;
    private ConsultarEvolucionPatrimonioService evolucion;
    private ConsultarRendimientosPatrimonioService rendimientos;
    private ConsultarLiquidezPatrimonioService liquidez;
    private ConsultarEventosPatrimonioService eventosServicio;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID portafolioId = UUID.randomUUID();
    private final UUID cuentaId = UUID.randomUUID();
    private final UUID instrumentoId = UUID.randomUUID();
    private final UUID claseId = UUID.randomUUID();
    private final UUID tipoId = UUID.randomUUID();
    private final UUID institucionId = UUID.randomUUID();

    private final MonedaResumen usd =
        new MonedaResumen(UUID.randomUUID(), "USD", "Dólar", "$", 2);
    private final MonedaResumen eur =
        new MonedaResumen(UUID.randomUUID(), "EUR", "Euro", "€", 2);

    @BeforeEach
    void preparar() {
        MonedasPatrimonio monedasPatrimonio = new MonedasPatrimonio(monedas);
        MovimientosPatrimonio movimientosPatrimonio =
            new MovimientosPatrimonio();
        IndicadoresPatrimonio indicadores = new IndicadoresPatrimonio();
        MetadatosPatrimonio metadatos = new MetadatosPatrimonio();
        CargadorContextoPatrimonial cargador = new CargadorContextoPatrimonial(
            new AccesoPatrimonio(accesoQueryPort),
            portafolios,
            cuentas,
            posiciones,
            movimientos);

        CalculadoraEventosPatrimonio calculadoraEventos =
            new CalculadoraEventosPatrimonio(eventosPort, monedasPatrimonio);
        CalculadoraLiquidezPatrimonio calculadoraLiquidez =
            new CalculadoraLiquidezPatrimonio(
                monedasPatrimonio, calculadoraEventos, indicadores);
        CalculadoraResumenPatrimonio calculadoraResumen =
            new CalculadoraResumenPatrimonio(
                movimientosPatrimonio, monedasPatrimonio, indicadores);
        CalculadoraDistribucionPatrimonio calculadoraDistribucion =
            new CalculadoraDistribucionPatrimonio(monedasPatrimonio);
        CalculadoraEvolucionPatrimonio calculadoraEvolucion =
            new CalculadoraEvolucionPatrimonio(
                monedasPatrimonio, valuaciones);
        CalculadoraRendimientosPatrimonio calculadoraRendimientos =
            new CalculadoraRendimientosPatrimonio(
                movimientosPatrimonio, monedasPatrimonio, indicadores);

        centro = new ConsultarCentroPatrimonioService(
            cargador,
            metadatos,
            calculadoraResumen,
            calculadoraDistribucion,
            calculadoraEvolucion,
            calculadoraRendimientos,
            calculadoraLiquidez,
            calculadoraEventos);
        resumen = new ConsultarResumenPatrimonioService(
            cargador, calculadoraResumen);
        distribucion = new ConsultarDistribucionPatrimonioService(
            cargador, calculadoraDistribucion);
        evolucion = new ConsultarEvolucionPatrimonioService(
            cargador, calculadoraEvolucion);
        rendimientos = new ConsultarRendimientosPatrimonioService(
            cargador, calculadoraRendimientos);
        liquidez = new ConsultarLiquidezPatrimonioService(
            cargador, calculadoraLiquidez);
        eventosServicio = new ConsultarEventosPatrimonioService(
            cargador, calculadoraEventos);
    }

    @Test
    void resumenSeparaCostoDeCapitalAportadoYNoSumaMonedas() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(
                posicion(usd, "100", true, "2025-01-01"),
                posicion(eur, "200", true, "2025-01-01")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimiento(usd, "30", false, null, "2025-01-10"),
                movimiento(eur, "50", false, null, "2025-01-10")));

        ResumenView vista = resumen.execute(consulta());

        assertThat(vista.activosRegistrados().estado())
            .isEqualTo(EstadoInformacion.PARCIAL);
        assertThat(vista.activosRegistrados().importes())
            .extracting(ImporteMonedaView::codigo)
            .containsExactly("EUR", "USD");
        assertThat(vista.capitalInvertido().importes())
            .extracting(ImporteMonedaView::codigo)
            .containsExactly("EUR", "USD");

        // Nunca existe un importe agregado sin moneda: 300 no aparece.
        assertThat(vista.activosRegistrados().importes())
            .extracting(ImporteMonedaView::importe)
            .containsExactly(new BigDecimal("200"), new BigDecimal("100"));

        assertThat(vista.capitalAportado().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(vista.capitalAportado().importes()).isEmpty();
        assertThat(vista.patrimonioNeto().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(vista.liquidezDisponible().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
    }

    @Test
    void resultadoRealizadoIncluyePosicionesCerradasYNoDuplicaReversiones() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        UUID original = UUID.randomUUID();
        UUID reversion = UUID.randomUUID();
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimiento(usd, "30", false, null, "2025-01-10", original),
                movimiento(usd, "-30", true, original, "2025-01-11", reversion),
                movimiento(usd, "40", false, null, "2025-02-10")));

        ResumenView vista = resumen.execute(consulta());

        assertThat(vista.resultadoRealizado().estado())
            .isEqualTo(EstadoInformacion.DISPONIBLE);
        assertThat(vista.resultadoRealizado().importes())
            .singleElement()
            .satisfies(importe -> assertThat(importe.importe())
                .isEqualByComparingTo("40"));
    }

    @Test
    void evolucionUsaElHistorialAnteriorComoSaldoDeAperturaSinInventarFechas() {
        prepararPortafolioUnico();
        when(valuaciones.disponible()).thenReturn(false);
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimientoCosto(usd, "100", "2025-01-01"),
                movimientoCosto(usd, "50", "2025-02-15")));

        EvolucionView vista = evolucion.execute(new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            LocalDate.of(2025, 2, 1),
            LocalDate.of(2025, 3, 1)));

        SerieEvolucionView costo = serie(vista, "COSTO_ADQUISICION");
        assertThat(costo.porMoneda()).singleElement().satisfies(serie -> {
            assertThat(serie.puntos()).extracting(punto -> punto.fecha())
                .containsExactly(
                    LocalDate.of(2025, 2, 28),
                    LocalDate.of(2025, 3, 1));
            assertThat(serie.puntos()).extracting(punto -> punto.valor())
                .containsExactly(
                    new BigDecimal("150"),
                    new BigDecimal("150"));
        });

        assertThat(serie(vista, "VALOR_MERCADO").estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(serie(vista, "PATRIMONIO_NETO").estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
    }

    @Test
    void laEvolucionNoMuestraPuntosAnterioresAlPrimerMovimiento() {
        prepararPortafolioUnico();
        when(valuaciones.disponible()).thenReturn(false);
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimientoCosto(usd, "100", "2025-02-15")));

        EvolucionView vista = evolucion.execute(new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 3, 1)));

        SerieEvolucionView costo = serie(vista, "COSTO_ADQUISICION");
        assertThat(costo.porMoneda()).singleElement().satisfies(serie ->
            assertThat(serie.puntos()).extracting(punto -> punto.fecha())
                .doesNotContain(LocalDate.of(2025, 1, 31))
                .contains(LocalDate.of(2025, 2, 28)));
    }

    @Test
    void distribucionCalculaPorcentajesDentroDeCadaMoneda() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(
                posicion(usd, "75", true, "2025-01-01",
                    UUID.randomUUID(), "Renta variable"),
                posicion(usd, "25", true, "2025-01-01",
                    UUID.randomUUID(), "Renta fija"),
                posicion(eur, "50", true, "2025-01-01")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());

        DistribucionView vista = distribucion.execute(
            new ConsultarPatrimonioQuery(
                usuarioId,
                portafolioId,
                null,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.CLASE_ACTIVO,
                null,
                null));

        assertThat(vista.porMoneda()).hasSize(2);
        assertThat(vista.porMoneda())
            .filteredOn(moneda -> moneda.moneda().codigo().equals("USD"))
            .singleElement()
            .satisfies(moneda -> {
                assertThat(moneda.total()).isEqualByComparingTo("100");
                assertThat(moneda.segmentos())
                    .extracting(segmento -> segmento.porcentaje())
                    .containsExactly(
                        new BigDecimal("75.0000"),
                        new BigDecimal("25.0000"));
            });
        assertThat(vista.porMoneda())
            .filteredOn(moneda -> moneda.moneda().codigo().equals("EUR"))
            .singleElement()
            .satisfies(moneda -> {
                assertThat(moneda.total()).isEqualByComparingTo("50");
                assertThat(moneda.segmentos())
                    .extracting(segmento -> segmento.porcentaje())
                    .containsExactly(new BigDecimal("100.0000"));
            });
    }

    @Test
    void distribucionPorInstitucionUsaLaCuentaDeLaPosicion() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(posicion(usd, "75", true, "2025-01-01")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());

        DistribucionView vista = distribucion.execute(
            new ConsultarPatrimonioQuery(
                usuarioId,
                portafolioId,
                null,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.INSTITUCION,
                null,
                null));

        assertThat(vista.porMoneda()).singleElement().satisfies(moneda ->
            assertThat(moneda.segmentos()).singleElement().satisfies(segmento -> {
                assertThat(segmento.clave())
                    .isEqualTo(institucionId.toString());
                assertThat(segmento.nombre()).isEqualTo("Banco");
            }));
    }

    @Test
    void unaPosicionQueReferenciaUnaCuentaInexistenteFalla() {
        prepararPortafolioUnico();
        UUID cuentaAjena = UUID.randomUUID();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(posicionEnCuenta(
                usd, "100", true, "2025-01-01", cuentaAjena)));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());

        assertThatThrownBy(() -> distribucion.execute(
            new ConsultarPatrimonioQuery(
                usuarioId,
                portafolioId,
                null,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.INSTITUCION,
                null,
                null)))
            .isInstanceOf(DatosPersistidosInvalidos.class);
    }

    @Test
    void unPortafolioSinCuentasNoConsultaPosiciones() {
        when(accesoQueryPort.buscarPermiso(usuarioId, portafolioId))
            .thenReturn(Optional.of(new PermisoPortafolio(
                portafolioId, true, true)));
        when(portafolios.buscarPorId(portafolioId))
            .thenReturn(Optional.of(portafolio()));
        when(cuentas.listarPorPortafolio(portafolioId)).thenReturn(List.of());
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());

        ResumenView vista = resumen.execute(consulta());

        assertThat(vista.activosRegistrados().importes()).isEmpty();
        assertThat(vista.resultadoRealizado().importes()).isEmpty();
        verify(posiciones, never())
            .listarPorCuentas(anyCollection(), anyBoolean());
    }

    @Test
    void elFiltroPorMonedaNoConvierteNiSumaOtrasMonedas() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(
                posicion(usd, "100", true, "2025-01-01"),
                posicion(eur, "200", true, "2025-01-01")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimiento(usd, "30", false, null, "2025-01-10"),
                movimiento(eur, "50", false, null, "2025-01-10")));

        ResumenView vista = resumen.execute(new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            "usd",
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            null,
            null));

        assertThat(vista.activosRegistrados().importes())
            .singleElement()
            .satisfies(importe -> {
                assertThat(importe.codigo()).isEqualTo("USD");
                assertThat(importe.importe()).isEqualByComparingTo("100");
            });
        assertThat(vista.resultadoRealizado().importes())
            .singleElement()
            .satisfies(importe -> {
                assertThat(importe.codigo()).isEqualTo("USD");
                assertThat(importe.importe()).isEqualByComparingTo("30");
            });
    }

    @Test
    void liquidezSeparaEfectivoNoDisponibleDeCapitalComprometido() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(posicion(usd, "100", true, "2025-01-01")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());
        when(eventosPort.disponible()).thenReturn(false);
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        LiquidezView vista = liquidez.execute(consulta());

        assertThat(vista.efectivoPorCuenta().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(vista.efectivoPorCuenta().cuentas()).singleElement()
            .satisfies(cuenta -> assertThat(cuenta.saldo()).isNull());
        assertThat(vista.disponibilidadInmediata().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(vista.capitalComprometido().estado())
            .isEqualTo(EstadoInformacion.PARCIAL);
        assertThat(vista.capitalComprometido().importes())
            .singleElement()
            .satisfies(importe ->
                assertThat(importe.importe()).isEqualByComparingTo("100"));
        assertThat(vista.proximosVencimientos().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
    }

    @Test
    void rendimientosFiltraPorPeriodoYExcluyeMovimientosRevertidos() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        UUID original = UUID.randomUUID();
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimientoOperacion(usd, "100", "5", "2", "2025-03-10"),
                movimientoOperacion(usd, "50", "1", "1", "2024-12-01"),
                movimiento(usd, "-100", true, original, "2025-03-11"),
                movimiento(usd, "0", false, null, "2025-03-12", original)));

        RendimientosView vista = rendimientos.execute(
            new ConsultarPatrimonioQuery(
                usuarioId,
                portafolioId,
                null,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.CLASE_ACTIVO,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)));

        assertThat(vista.resultadoRealizado().importes())
            .singleElement()
            .satisfies(importe ->
                assertThat(importe.importe()).isEqualByComparingTo("100"));
        assertThat(vista.comisionesPagadas().importes())
            .singleElement()
            .satisfies(importe ->
                assertThat(importe.importe()).isEqualByComparingTo("5"));
        assertThat(vista.impuestosPagados().importes())
            .singleElement()
            .satisfies(importe ->
                assertThat(importe.importe()).isEqualByComparingTo("2"));
    }

    @Test
    void eventosMapeaLaMonedaYOrdenaPorFecha() {
        prepararPortafolioUnico();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());
        when(eventosPort.disponible()).thenReturn(true);
        when(eventosPort.listar(eq(portafolioId), any(), any()))
            .thenReturn(List.of(new EventoPatrimonio(
                UUID.randomUUID(),
                portafolioId,
                "VENCIMIENTO",
                LocalDate.of(2025, 6, 1),
                "Vence",
                new BigDecimal("10"),
                usd.id(),
                "CONTRATO",
                null,
                null)));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        EventosView vista = eventosServicio.execute(consulta());

        assertThat(vista.estado()).isEqualTo(EstadoInformacion.DISPONIBLE);
        assertThat(vista.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.tipo()).isEqualTo("VENCIMIENTO");
            assertThat(evento.moneda().codigo()).isEqualTo("USD");
        });
    }

    @Test
    void elCentroGlobalSoloConsultaPortafoliosAutorizadosYComponeTodo() {
        when(valuaciones.disponible()).thenReturn(false);
        when(eventosPort.disponible()).thenReturn(false);
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));
        when(accesoQueryPort.listarAutorizados(usuarioId))
            .thenReturn(List.of(portafolio()));
        when(cuentas.listarPorPortafolio(portafolioId))
            .thenReturn(List.of(cuenta()));
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(posicion(usd, "100", true, "2025-01-01")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimientoOperacion(usd, "30", "0", "0", "2025-01-10")));

        CentroView vista = centro.execute(new ConsultarPatrimonioQuery(
            usuarioId,
            null,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            null,
            null));

        verify(accesoQueryPort).listarAutorizados(usuarioId);
        verify(cuentas).listarPorPortafolio(portafolioId);
        verify(movimientos).listarPorPortafolio(
            eq(portafolioId), any(), any());

        assertThat(vista.meta().portafolios()).hasSize(1);
        assertThat(vista.resumen().activosRegistrados().importes())
            .isNotEmpty();
        assertThat(vista.distribucion().porMoneda()).hasSize(1);
        assertThat(vista.evolucion().series()).isNotEmpty();
        assertThat(vista.rendimientos().resultadoRealizado().importes())
            .isNotEmpty();
        assertThat(vista.liquidez().capitalComprometido().importes())
            .isNotEmpty();
        assertThat(vista.eventos().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
    }

    @Test
    void unPortafolioAjenoSinMembresiaDevuelveNoEncontrado() {
        UUID ajeno = UUID.randomUUID();
        when(accesoQueryPort.buscarPermiso(usuarioId, ajeno))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumen.execute(new ConsultarPatrimonioQuery(
            usuarioId,
            ajeno,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            null,
            null)))
            .isInstanceOf(NotFoundException.class);

        verify(portafolios, never()).buscarPorId(any());
    }

    private void prepararPortafolioUnico() {
        when(accesoQueryPort.buscarPermiso(usuarioId, portafolioId))
            .thenReturn(Optional.of(new PermisoPortafolio(
                portafolioId, true, true)));
        when(portafolios.buscarPorId(portafolioId))
            .thenReturn(Optional.of(portafolio()));
        when(cuentas.listarPorPortafolio(portafolioId))
            .thenReturn(List.of(cuenta()));
    }

    private ConsultarPatrimonioQuery consulta() {
        return new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            null,
            null);
    }

    private PortafolioResumen portafolio() {
        return new PortafolioResumen(
            portafolioId,
            "Portafolio",
            null,
            usd.id(),
            usd.codigo(),
            usd.nombre(),
            usd.simbolo(),
            usd.decimales(),
            true);
    }

    private CuentaPatrimonio cuenta() {
        return new CuentaPatrimonio(
            cuentaId,
            portafolioId,
            "Cuenta",
            institucionId,
            "Banco",
            UUID.randomUUID(),
            "Cuenta de corretaje",
            usd.id(),
            true);
    }

    private PosicionPatrimonio posicion(
        MonedaResumen moneda,
        String costo,
        boolean activa,
        String fechaApertura
    ) {
        return posicion(
            moneda, costo, activa, fechaApertura, claseId, "Renta variable");
    }

    private PosicionPatrimonio posicion(
        MonedaResumen moneda,
        String costo,
        boolean activa,
        String fechaApertura,
        UUID clase,
        String nombreClase
    ) {
        return posicionEnCuenta(
            moneda, costo, activa, fechaApertura, cuentaId, clase,
            nombreClase);
    }

    private PosicionPatrimonio posicionEnCuenta(
        MonedaResumen moneda,
        String costo,
        boolean activa,
        String fechaApertura,
        UUID cuenta
    ) {
        return posicionEnCuenta(
            moneda, costo, activa, fechaApertura, cuenta, claseId,
            "Renta variable");
    }

    private PosicionPatrimonio posicionEnCuenta(
        MonedaResumen moneda,
        String costo,
        boolean activa,
        String fechaApertura,
        UUID cuenta,
        UUID clase,
        String nombreClase
    ) {
        return new PosicionPatrimonio(
            UUID.randomUUID(),
            cuenta,
            instrumentoId,
            "Apple",
            "AAPL",
            clase,
            nombreClase,
            tipoId,
            "Acción",
            new BigDecimal("10"),
            new BigDecimal(costo),
            BigDecimal.ZERO,
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales(),
            activa,
            LocalDate.parse(fechaApertura),
            null);
    }

    private MovimientoPatrimonio movimiento(
        MonedaResumen moneda,
        String resultado,
        boolean reversion,
        UUID origen,
        String fecha
    ) {
        return movimiento(
            moneda, resultado, reversion, origen, fecha, UUID.randomUUID());
    }

    private MovimientoPatrimonio movimiento(
        MonedaResumen moneda,
        String resultado,
        boolean reversion,
        UUID origen,
        String fecha,
        UUID id
    ) {
        return movimiento(
            moneda, resultado, reversion, origen, fecha, id, "0", "0");
    }

    private MovimientoPatrimonio movimientoOperacion(
        MonedaResumen moneda,
        String resultado,
        String comisiones,
        String impuestos,
        String fecha
    ) {
        return movimiento(
            moneda,
            resultado,
            false,
            null,
            fecha,
            UUID.randomUUID(),
            comisiones,
            impuestos);
    }

    private MovimientoPatrimonio movimiento(
        MonedaResumen moneda,
        String resultado,
        boolean reversion,
        UUID origen,
        String fecha,
        UUID id,
        String comisiones,
        String impuestos
    ) {
        return new MovimientoPatrimonio(
            id,
            portafolioId,
            cuentaId,
            UUID.randomUUID(),
            instrumentoId,
            claseId,
            "Renta variable",
            tipoId,
            "Acción",
            reversion ? "REVERSION" : "VENTA",
            reversion,
            LocalDate.parse(fecha),
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales(),
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            new BigDecimal(comisiones),
            new BigDecimal(impuestos),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            new BigDecimal(resultado),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            origen,
            Instant.parse("2025-01-10T00:00:00Z"));
    }

    private MovimientoPatrimonio movimientoCosto(
        MonedaResumen moneda,
        String costo,
        String fecha
    ) {
        return new MovimientoPatrimonio(
            UUID.randomUUID(),
            portafolioId,
            cuentaId,
            UUID.randomUUID(),
            instrumentoId,
            claseId,
            "Renta variable",
            tipoId,
            "Acción",
            "COMPRA",
            false,
            LocalDate.parse(fecha),
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales(),
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ONE,
            new BigDecimal(costo),
            null,
            Instant.parse("2025-01-01T00:00:00Z"));
    }

    private static SerieEvolucionView serie(
        EvolucionView evolucion,
        String codigo
    ) {
        return evolucion.series().stream()
            .filter(serie -> serie.codigo().equals(codigo))
            .findFirst()
            .orElseThrow();
    }
}
