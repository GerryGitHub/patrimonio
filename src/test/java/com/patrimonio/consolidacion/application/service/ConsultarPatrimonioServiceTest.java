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
import com.patrimonio.consolidacion.application.port.out.EventoPatrimonioPort;
import com.patrimonio.consolidacion.application.port.out.ValuacionPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.EvolucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.ImporteMonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.IndicadorView;
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
 * Pruebas de las reglas financieras del Centro de Patrimonio.
 *
 * <p>Cubren la separación entre costo y capital aportado, la ausencia de suma
 * entre monedas, el resultado realizado sin doble conteo de reversiones, la
 * evolución sin datos inventados y la autorización global por membresía.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConsultarPatrimonioServiceTest {

    private static final LocalDate HOY = LocalDate.now();

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
    private EventoPatrimonioPort eventos;

    private ConsultarPatrimonioService servicio;

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
        servicio = new ConsultarPatrimonioService(
            new AccesoPatrimonio(accesoQueryPort),
            portafolios,
            cuentas,
            posiciones,
            movimientos,
            monedas,
            valuaciones,
            eventos);
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

        ResumenView resumen = servicio.resumen(consulta());

        assertThat(resumen.activosRegistrados().estado())
            .isEqualTo(EstadoInformacion.PARCIAL);
        assertThat(resumen.activosRegistrados().importes())
            .extracting(ImporteMonedaView::codigo)
            .containsExactly("EUR", "USD");
        assertThat(resumen.capitalInvertido().importes())
            .extracting(ImporteMonedaView::codigo)
            .containsExactly("EUR", "USD");

        // Nunca existe un importe agregado sin moneda: 300 no aparece.
        assertThat(resumen.activosRegistrados().importes())
            .extracting(ImporteMonedaView::importe)
            .containsExactly(new BigDecimal("200"), new BigDecimal("100"));

        assertThat(resumen.capitalAportado().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(resumen.capitalAportado().importes()).isEmpty();
        assertThat(resumen.patrimonioNeto().estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(resumen.liquidezDisponible().estado())
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

        ResumenView resumen = servicio.resumen(consulta());

        assertThat(resumen.resultadoRealizado().estado())
            .isEqualTo(EstadoInformacion.DISPONIBLE);
        assertThat(resumen.resultadoRealizado().importes())
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

        EvolucionView evolucion = servicio.evolucion(new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            LocalDate.of(2025, 2, 1),
            LocalDate.of(2025, 3, 1)));

        SerieEvolucionView costo = serie(evolucion, "COSTO_ADQUISICION");
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

        assertThat(serie(evolucion, "VALOR_MERCADO").estado())
            .isEqualTo(EstadoInformacion.NO_DISPONIBLE);
        assertThat(serie(evolucion, "PATRIMONIO_NETO").estado())
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

        EvolucionView evolucion = servicio.evolucion(new ConsultarPatrimonioQuery(
            usuarioId,
            portafolioId,
            null,
            PeriodoPatrimonial.TODO,
            DimensionDistribucion.CLASE_ACTIVO,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 3, 1)));

        SerieEvolucionView costo = serie(evolucion, "COSTO_ADQUISICION");
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

        DistribucionView distribucion = servicio.distribucion(
            new ConsultarPatrimonioQuery(
                usuarioId,
                portafolioId,
                null,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.CLASE_ACTIVO,
                null,
                null));

        assertThat(distribucion.porMoneda()).hasSize(2);
        assertThat(distribucion.porMoneda())
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
        assertThat(distribucion.porMoneda())
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
    void elCentroGlobalSoloConsultaPortafoliosAutorizados() {
        when(valuaciones.disponible()).thenReturn(false);
        when(eventos.disponible()).thenReturn(false);
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));
        when(accesoQueryPort.listarAutorizados(usuarioId))
            .thenReturn(List.of(portafolio()));
        when(cuentas.listarPorPortafolio(portafolioId))
            .thenReturn(List.of(cuenta()));
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());

        servicio.centro(new ConsultarPatrimonioQuery(
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
    }

    @Test
    void unPortafolioAjenoSinMembresiaDevuelveNoEncontrado() {
        UUID ajeno = UUID.randomUUID();
        when(accesoQueryPort.buscarPermiso(usuarioId, ajeno))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.resumen(new ConsultarPatrimonioQuery(
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
        return new PosicionPatrimonio(
            UUID.randomUUID(),
            cuentaId,
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
        return movimiento(moneda, resultado, reversion, origen, fecha,
            UUID.randomUUID());
    }

    private MovimientoPatrimonio movimiento(
        MonedaResumen moneda,
        String resultado,
        boolean reversion,
        UUID origen,
        String fecha,
        UUID id
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
            BigDecimal.ZERO,
            BigDecimal.ZERO,
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
