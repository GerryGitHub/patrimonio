package com.patrimonio.consolidacion.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.consolidacion.application.command.CrearSnapshotPatrimonioCommand;
import com.patrimonio.consolidacion.application.port.out.SnapshotPatrimonioRepository;
import com.patrimonio.consolidacion.application.query.ConsultarSnapshotsQuery;
import com.patrimonio.consolidacion.application.support.AccesoPatrimonio;
import com.patrimonio.consolidacion.application.support.MovimientosPatrimonio;
import com.patrimonio.consolidacion.application.support.SnapshotPatrimonioViewAssembler;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SnapshotView;
import com.patrimonio.consolidacion.domain.error.SnapshotInvalidoException;
import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.cuentas.api.port.CuentaPatrimonioPort;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.inversiones.api.port.MovimientoPatrimonioPort;
import com.patrimonio.inversiones.api.port.PosicionPatrimonioPort;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas de los casos de uso de snapshots patrimoniales.
 *
 * <p>Cubren idempotencia por clave, autorización de lectura y escritura,
 * rechazo de fechas no actuales, agrupación por moneda, exclusión de
 * movimientos revertidos y mapeo de la vista.</p>
 */
@ExtendWith(MockitoExtension.class)
class SnapshotPatrimonioServicesTest {

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
    private SnapshotPatrimonioRepository snapshots;

    private CrearSnapshotPatrimonioService crear;
    private ListarSnapshotsPatrimonioService listar;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID portafolioId = UUID.randomUUID();
    private final UUID cuentaId = UUID.randomUUID();
    private final UUID instrumentoId = UUID.randomUUID();
    private final UUID claseId = UUID.randomUUID();
    private final UUID tipoId = UUID.randomUUID();
    private final LocalDate hoy = LocalDate.now(ZoneOffset.UTC);

    private final MonedaResumen usd =
        new MonedaResumen(UUID.randomUUID(), "USD", "Dólar", "$", 2);
    private final MonedaResumen eur =
        new MonedaResumen(UUID.randomUUID(), "EUR", "Euro", "€", 2);

    @BeforeEach
    void preparar() {
        AccesoPatrimonio acceso = new AccesoPatrimonio(accesoQueryPort);
        SnapshotPatrimonioViewAssembler assembler =
            new SnapshotPatrimonioViewAssembler(monedas);
        crear = new CrearSnapshotPatrimonioService(
            acceso,
            portafolios,
            cuentas,
            posiciones,
            movimientos,
            snapshots,
            new MovimientosPatrimonio(),
            assembler);
        listar = new ListarSnapshotsPatrimonioService(
            acceso, snapshots, assembler);
    }

    @Test
    void crearGuardaCapitalYResultadoSinRellenarCamposInexistentes() {
        permitirEscritura();
        prepararPortafolioConCuenta();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(posicion(usd, "100")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(movimiento(usd, "30", false, null)));
        when(snapshots.buscarPorClave(
            portafolioId, hoy, usd.id(), SnapshotPatrimonio.FUENTE_MANUAL))
            .thenReturn(Optional.empty());
        when(snapshots.guardar(any(SnapshotPatrimonio.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        List<SnapshotView> vistas = crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, null));

        assertThat(vistas).singleElement().satisfies(vista -> {
            assertThat(vista.capitalInvertido())
                .isEqualByComparingTo("100");
            assertThat(vista.resultadoRealizado())
                .isEqualByComparingTo("30");
            assertThat(vista.efectivo()).isNull();
            assertThat(vista.pasivos()).isNull();
            assertThat(vista.valorMercado()).isNull();
            assertThat(vista.patrimonioNeto()).isNull();
            assertThat(vista.cobertura())
                .isEqualTo(SnapshotPatrimonio.COBERTURA_INVERSIONES);
            assertThat(vista.fuente())
                .isEqualTo(SnapshotPatrimonio.FUENTE_MANUAL);
            assertThat(vista.moneda().codigo()).isEqualTo("USD");
        });
        verify(snapshots).guardar(any(SnapshotPatrimonio.class));
    }

    @Test
    void repetirLaCreacionDevuelveElSnapshotExistenteSinGuardar() {
        permitirEscritura();
        prepararPortafolioConCuenta();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(posicion(usd, "100")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());
        SnapshotPatrimonio existente = snapshot(usd, "100", "0");
        when(snapshots.buscarPorClave(
            portafolioId, hoy, usd.id(), SnapshotPatrimonio.FUENTE_MANUAL))
            .thenReturn(Optional.of(existente));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        List<SnapshotView> vistas = crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, hoy));

        assertThat(vistas).singleElement()
            .satisfies(vista -> assertThat(vista.id())
                .isEqualTo(existente.id()));
        verify(snapshots, never()).guardar(any(SnapshotPatrimonio.class));
    }

    @Test
    void unaFechaPasadaSeRechazaSinConsultarDatos() {
        permitirEscritura();
        when(portafolios.buscarPorId(portafolioId))
            .thenReturn(Optional.of(portafolio()));

        assertThatThrownBy(() -> crear.execute(
            new CrearSnapshotPatrimonioCommand(
                usuarioId, portafolioId, hoy.minusDays(1))))
            .isInstanceOf(SnapshotInvalidoException.class);

        verifyNoInteractions(cuentas, posiciones, movimientos, snapshots);
    }

    @Test
    void unaFechaFuturaSeRechazaSinConsultarDatos() {
        permitirEscritura();
        when(portafolios.buscarPorId(portafolioId))
            .thenReturn(Optional.of(portafolio()));

        assertThatThrownBy(() -> crear.execute(
            new CrearSnapshotPatrimonioCommand(
                usuarioId, portafolioId, hoy.plusDays(1))))
            .isInstanceOf(SnapshotInvalidoException.class);

        verifyNoInteractions(cuentas, posiciones, movimientos, snapshots);
    }

    @Test
    void crearSinMembresiaLanzaNoEncontrado() {
        when(accesoQueryPort.buscarPermiso(usuarioId, portafolioId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, null)))
            .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(
            portafolios, cuentas, posiciones, movimientos, snapshots);
    }

    @Test
    void crearSinCapacidadDeEscrituraLanzaAccesoDenegado() {
        when(accesoQueryPort.buscarPermiso(usuarioId, portafolioId))
            .thenReturn(Optional.of(
                new PermisoPortafolio(portafolioId, true, false)));

        assertThatThrownBy(() -> crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, null)))
            .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(
            portafolios, cuentas, posiciones, movimientos, snapshots);
    }

    @Test
    void unPortafolioSinDatosGeneraUnSnapshotEnSuMonedaBase() {
        permitirEscritura();
        when(portafolios.buscarPorId(portafolioId))
            .thenReturn(Optional.of(portafolio()));
        when(cuentas.listarPorPortafolio(portafolioId)).thenReturn(List.of());
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of());
        when(snapshots.buscarPorClave(
            portafolioId, hoy, usd.id(), SnapshotPatrimonio.FUENTE_MANUAL))
            .thenReturn(Optional.empty());
        when(snapshots.guardar(any(SnapshotPatrimonio.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        List<SnapshotView> vistas = crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, null));

        assertThat(vistas).singleElement().satisfies(vista -> {
            assertThat(vista.capitalInvertido()).isEqualByComparingTo("0");
            assertThat(vista.resultadoRealizado()).isEqualByComparingTo("0");
            assertThat(vista.moneda().codigo()).isEqualTo("USD");
        });
        verify(posiciones, never())
            .listarPorCuentas(anyCollection(), anyBoolean());
    }

    @Test
    void crearAgrupaCapitalYResultadoPorMoneda() {
        permitirEscritura();
        prepararPortafolioConCuenta();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of(
                posicion(usd, "100"),
                posicion(eur, "200")));
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimiento(usd, "30", false, null),
                movimiento(eur, "50", false, null)));
        when(snapshots.buscarPorClave(
            eq(portafolioId), eq(hoy), any(), eq(SnapshotPatrimonio.FUENTE_MANUAL)))
            .thenReturn(Optional.empty());
        when(snapshots.guardar(any(SnapshotPatrimonio.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));
        when(monedas.buscarPorId(eur.id())).thenReturn(Optional.of(eur));

        List<SnapshotView> vistas = crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, null));

        assertThat(vistas).hasSize(2);
        assertThat(vistas)
            .filteredOn(vista -> vista.moneda().codigo().equals("USD"))
            .singleElement()
            .satisfies(vista -> {
                assertThat(vista.capitalInvertido())
                    .isEqualByComparingTo("100");
                assertThat(vista.resultadoRealizado())
                    .isEqualByComparingTo("30");
            });
        assertThat(vistas)
            .filteredOn(vista -> vista.moneda().codigo().equals("EUR"))
            .singleElement()
            .satisfies(vista -> {
                assertThat(vista.capitalInvertido())
                    .isEqualByComparingTo("200");
                assertThat(vista.resultadoRealizado())
                    .isEqualByComparingTo("50");
            });
    }

    @Test
    void crearExcluyeMovimientosRevertidosYOriginalesRevertidos() {
        permitirEscritura();
        prepararPortafolioConCuenta();
        when(posiciones.listarPorCuentas(anyCollection(), eq(true)))
            .thenReturn(List.of());
        UUID original = UUID.randomUUID();
        when(movimientos.listarPorPortafolio(eq(portafolioId), any(), any()))
            .thenReturn(List.of(
                movimientoConId(usd, "30", false, null, original),
                movimiento(usd, "-30", true, original),
                movimiento(usd, "40", false, null)));
        when(snapshots.buscarPorClave(
            portafolioId, hoy, usd.id(), SnapshotPatrimonio.FUENTE_MANUAL))
            .thenReturn(Optional.empty());
        when(snapshots.guardar(any(SnapshotPatrimonio.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        List<SnapshotView> vistas = crear.execute(
            new CrearSnapshotPatrimonioCommand(usuarioId, portafolioId, null));

        assertThat(vistas).singleElement().satisfies(vista ->
            assertThat(vista.resultadoRealizado())
                .isEqualByComparingTo("40"));
    }

    @Test
    void listarExigeLecturaYMapeaLaVista() {
        when(accesoQueryPort.buscarPermiso(usuarioId, portafolioId))
            .thenReturn(Optional.of(
                new PermisoPortafolio(portafolioId, true, true)));
        SnapshotPatrimonio existente = snapshot(usd, "100", "30");
        when(snapshots.listarPorPortafolio(portafolioId))
            .thenReturn(List.of(existente));
        when(monedas.buscarPorId(usd.id())).thenReturn(Optional.of(usd));

        List<SnapshotView> vistas = listar.execute(
            new ConsultarSnapshotsQuery(usuarioId, portafolioId));

        assertThat(vistas).singleElement().satisfies(vista -> {
            assertThat(vista.id()).isEqualTo(existente.id());
            assertThat(vista.moneda().codigo()).isEqualTo("USD");
            assertThat(vista.capitalInvertido())
                .isEqualByComparingTo("100");
            assertThat(vista.resultadoRealizado())
                .isEqualByComparingTo("30");
        });
        verify(snapshots, never()).guardar(any(SnapshotPatrimonio.class));
    }

    private void permitirEscritura() {
        when(accesoQueryPort.buscarPermiso(usuarioId, portafolioId))
            .thenReturn(Optional.of(
                new PermisoPortafolio(portafolioId, true, true)));
    }

    private void prepararPortafolioConCuenta() {
        when(portafolios.buscarPorId(portafolioId))
            .thenReturn(Optional.of(portafolio()));
        when(cuentas.listarPorPortafolio(portafolioId))
            .thenReturn(List.of(cuenta()));
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
            UUID.randomUUID(),
            "Banco",
            UUID.randomUUID(),
            "Cuenta de corretaje",
            usd.id(),
            true);
    }

    private SnapshotPatrimonio snapshot(
        MonedaResumen moneda,
        String capital,
        String resultado
    ) {
        return SnapshotPatrimonio.crear(
            UUID.randomUUID(),
            portafolioId,
            hoy,
            moneda.id(),
            new BigDecimal(capital),
            new BigDecimal(resultado),
            SnapshotPatrimonio.COBERTURA_INVERSIONES,
            SnapshotPatrimonio.LIMITACIONES_POR_DEFECTO,
            Instant.now());
    }

    private PosicionPatrimonio posicion(MonedaResumen moneda, String costo) {
        return new PosicionPatrimonio(
            UUID.randomUUID(),
            cuentaId,
            instrumentoId,
            "Apple",
            "AAPL",
            claseId,
            "Renta variable",
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
            true,
            LocalDate.parse("2025-01-01"),
            null);
    }

    private MovimientoPatrimonio movimiento(
        MonedaResumen moneda,
        String resultado,
        boolean reversion,
        UUID origen
    ) {
        return movimientoConId(
            moneda, resultado, reversion, origen, UUID.randomUUID());
    }

    private MovimientoPatrimonio movimientoConId(
        MonedaResumen moneda,
        String resultado,
        boolean reversion,
        UUID origen,
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
            hoy,
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
}
