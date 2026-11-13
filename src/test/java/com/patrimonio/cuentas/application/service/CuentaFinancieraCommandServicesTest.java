package com.patrimonio.cuentas.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.cuentas.application.command.ActualizarCuentaCommand;
import com.patrimonio.cuentas.application.command.CambiarEstadoCuentaCommand;
import com.patrimonio.cuentas.application.command.CrearCuentaCommand;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.support.AccesoPortafolio;
import com.patrimonio.cuentas.application.support.ValidadorReferenciaCuenta;
import com.patrimonio.cuentas.application.support.ValidadorRelacionesCuenta;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.error.CuentaDuplicadaException;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CuentaFinancieraCommandServicesTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PORTAFOLIO_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTRO_PORTAFOLIO_ID =
        UUID.fromString("11111111-2222-1111-1111-111111111111");
    private static final UUID CUENTA_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INSTITUCION_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID OTRA_INSTITUCION_ID =
        UUID.fromString("33333333-4444-3333-3333-333333333333");
    private static final UUID TIPO_CUENTA_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MONEDA_ID =
        UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private AccesoQueryPort accesoQueryPort;

    @Mock
    private CuentaFinancieraRepository cuentas;

    @Mock
    private ValidadorRelacionesCuenta validador;

    private CrearCuentaService crear;
    private ActualizarCuentaService actualizar;
    private CambiarEstadoCuentaService cambiarEstado;

    @BeforeEach
    void setUp() {
        AccesoPortafolio accesoPortafolio = new AccesoPortafolio(accesoQueryPort);
        ValidadorReferenciaCuenta validadorReferencia =
            new ValidadorReferenciaCuenta(cuentas);
        crear = new CrearCuentaService(
            accesoPortafolio, cuentas, validador, validadorReferencia);
        actualizar = new ActualizarCuentaService(
            accesoPortafolio, cuentas, validador, validadorReferencia);
        cambiarEstado = new CambiarEstadoCuentaService(accesoPortafolio, cuentas);
    }

    @Test
    void crear_conPermisoDeEscritura_guardaLaCuentaNormalizada() {
        permitirEscritura();
        when(cuentas.guardar(any(CuentaFinanciera.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        CuentaFinancieraView vista = crear.execute(new CrearCuentaCommand(
            USUARIO_ID,
            PORTAFOLIO_ID,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            MONEDA_ID,
            "  Cuenta   corriente ",
            "  Descripción  ",
            " ES91 2100 0418 4502 0005 1332 ",
            LocalDate.of(2024, 1, 15)));

        assertThat(vista.nombre()).isEqualTo("Cuenta corriente");
        assertThat(vista.descripcion()).isEqualTo("Descripción");
        assertThat(vista.numeroCuenta()).isEqualTo("ES91 2100 0418 4502 0005 1332");
        assertThat(vista.institucionId()).isEqualTo(INSTITUCION_ID);
        assertThat(vista.activo()).isTrue();
        verify(validador).validar(INSTITUCION_ID, TIPO_CUENTA_ID, MONEDA_ID);
    }

    @Test
    void crear_conNumeroDuplicadoEnLaInstitucion_falla() {
        permitirEscritura();
        when(cuentas.existeNumeroEnInstitucion(
            INSTITUCION_ID, "123456", null)).thenReturn(true);

        assertThatThrownBy(() -> crear.execute(new CrearCuentaCommand(
            USUARIO_ID, PORTAFOLIO_ID, INSTITUCION_ID, TIPO_CUENTA_ID,
            MONEDA_ID, "Cuenta", null, "123456", null)))
            .isInstanceOf(CuentaDuplicadaException.class);
    }

    @Test
    void crear_sinNumero_noCompruebaDuplicados() {
        permitirEscritura();
        when(cuentas.guardar(any(CuentaFinanciera.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        crear.execute(new CrearCuentaCommand(
            USUARIO_ID, PORTAFOLIO_ID, INSTITUCION_ID, TIPO_CUENTA_ID,
            MONEDA_ID, "Cuenta", null, "   ", null));

        verify(cuentas, org.mockito.Mockito.never())
            .existeNumeroEnInstitucion(any(), any(), any());
    }

    @Test
    void crear_sinMembresia_lanzaNotFoundSinValidarCatalogos() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> crear.execute(new CrearCuentaCommand(
            USUARIO_ID, PORTAFOLIO_ID, INSTITUCION_ID, TIPO_CUENTA_ID,
            MONEDA_ID, "Cuenta", null, null, null)))
            .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(validador);
        verifyNoInteractions(cuentas);
    }

    @Test
    void crear_sinCapacidadDeEscritura_lanzaAccesoDenegado() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(
                new PermisoPortafolio(PORTAFOLIO_ID, true, false)));

        assertThatThrownBy(() -> crear.execute(new CrearCuentaCommand(
            USUARIO_ID, PORTAFOLIO_ID, INSTITUCION_ID, TIPO_CUENTA_ID,
            MONEDA_ID, "Cuenta", null, null, null)))
            .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(validador);
        verifyNoInteractions(cuentas);
    }

    @Test
    void actualizar_modificaLosDatosEditables() {
        permitirEscritura();
        CuentaFinanciera existente = cuenta(PORTAFOLIO_ID, true, null);
        when(cuentas.buscarPorId(CUENTA_ID)).thenReturn(Optional.of(existente));
        when(cuentas.existeNumeroEnInstitucion(
            OTRA_INSTITUCION_ID, "9999", CUENTA_ID)).thenReturn(false);
        when(cuentas.guardar(any(CuentaFinanciera.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        CuentaFinancieraView vista = actualizar.execute(new ActualizarCuentaCommand(
            USUARIO_ID,
            PORTAFOLIO_ID,
            CUENTA_ID,
            OTRA_INSTITUCION_ID,
            TIPO_CUENTA_ID,
            MONEDA_ID,
            "Cuenta   nueva",
            "Detalle",
            "9999",
            LocalDate.of(2024, 2, 1)));

        assertThat(vista.institucionId()).isEqualTo(OTRA_INSTITUCION_ID);
        assertThat(vista.nombre()).isEqualTo("Cuenta nueva");
        assertThat(vista.descripcion()).isEqualTo("Detalle");
        assertThat(vista.numeroCuenta()).isEqualTo("9999");
        assertThat(vista.fechaApertura()).isEqualTo(LocalDate.of(2024, 2, 1));
        verify(validador).validar(OTRA_INSTITUCION_ID, TIPO_CUENTA_ID, MONEDA_ID);
    }

    @Test
    void actualizar_cuandoLaCuentaEsDeOtroPortafolio_lanzaNotFound() {
        permitirEscritura();
        when(cuentas.buscarPorId(CUENTA_ID))
            .thenReturn(Optional.of(cuenta(OTRO_PORTAFOLIO_ID, true, null)));

        assertThatThrownBy(() -> actualizar.execute(new ActualizarCuentaCommand(
            USUARIO_ID, PORTAFOLIO_ID, CUENTA_ID, INSTITUCION_ID,
            TIPO_CUENTA_ID, MONEDA_ID, "Cuenta", null, null, null)))
            .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(validador);
    }

    @Test
    void actualizar_sinNuevaReferencia_conservaLaExistente() {
        permitirEscritura();
        when(cuentas.buscarPorId(CUENTA_ID))
            .thenReturn(Optional.of(cuenta(PORTAFOLIO_ID, true, null)));
        when(cuentas.existeNumeroEnInstitucion(
            OTRA_INSTITUCION_ID, "1234", CUENTA_ID)).thenReturn(false);
        when(cuentas.guardar(any(CuentaFinanciera.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        CuentaFinancieraView vista = actualizar.execute(new ActualizarCuentaCommand(
            USUARIO_ID,
            PORTAFOLIO_ID,
            CUENTA_ID,
            OTRA_INSTITUCION_ID,
            TIPO_CUENTA_ID,
            MONEDA_ID,
            "Cuenta editada",
            null,
            "   ",
            null));

        assertThat(vista.numeroCuenta()).isEqualTo("1234");
        verify(cuentas).existeNumeroEnInstitucion(
            OTRA_INSTITUCION_ID, "1234", CUENTA_ID);
    }

    @Test
    void actualizar_conNumeroDuplicadoEnOtraCuenta_falla() {
        permitirEscritura();
        when(cuentas.buscarPorId(CUENTA_ID))
            .thenReturn(Optional.of(cuenta(PORTAFOLIO_ID, true, null)));
        when(cuentas.existeNumeroEnInstitucion(
            INSTITUCION_ID, "123456", CUENTA_ID)).thenReturn(true);

        assertThatThrownBy(() -> actualizar.execute(new ActualizarCuentaCommand(
            USUARIO_ID, PORTAFOLIO_ID, CUENTA_ID, INSTITUCION_ID,
            TIPO_CUENTA_ID, MONEDA_ID, "Cuenta", null, "123456", null)))
            .isInstanceOf(CuentaDuplicadaException.class);
    }

    @Test
    void cambiarEstado_aInactivoFijaLaFechaDeCierre() {
        permitirEscritura();
        when(cuentas.buscarPorId(CUENTA_ID))
            .thenReturn(Optional.of(cuenta(PORTAFOLIO_ID, true, null)));
        when(cuentas.guardar(any(CuentaFinanciera.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        CuentaFinancieraView vista = cambiarEstado.execute(
            new CambiarEstadoCuentaCommand(
                USUARIO_ID, PORTAFOLIO_ID, CUENTA_ID, false));

        assertThat(vista.activo()).isFalse();
        assertThat(vista.fechaCierre()).isEqualTo(LocalDate.now());
    }

    @Test
    void cambiarEstado_aActivoLimpiaLaFechaDeCierre() {
        permitirEscritura();
        when(cuentas.buscarPorId(CUENTA_ID)).thenReturn(Optional.of(
            cuenta(PORTAFOLIO_ID, false, LocalDate.of(2025, 1, 31))));
        when(cuentas.guardar(any(CuentaFinanciera.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        CuentaFinancieraView vista = cambiarEstado.execute(
            new CambiarEstadoCuentaCommand(
                USUARIO_ID, PORTAFOLIO_ID, CUENTA_ID, true));

        assertThat(vista.activo()).isTrue();
        assertThat(vista.fechaCierre()).isNull();
    }

    @Test
    void cambiarEstado_sinEscritura_lanzaAccesoDenegado() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(
                new PermisoPortafolio(PORTAFOLIO_ID, true, false)));

        assertThatThrownBy(() -> cambiarEstado.execute(
            new CambiarEstadoCuentaCommand(
                USUARIO_ID, PORTAFOLIO_ID, CUENTA_ID, false)))
            .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(cuentas);
    }

    private void permitirEscritura() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(
                new PermisoPortafolio(PORTAFOLIO_ID, true, true)));
    }

    private static CuentaFinanciera cuenta(
        UUID portafolioId,
        boolean activo,
        LocalDate fechaCierre
    ) {
        return CuentaFinanciera.reconstruir(
            CUENTA_ID,
            portafolioId,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            "Cuenta existente",
            null,
            new NumeroCuenta("1234"),
            MONEDA_ID,
            activo,
            LocalDate.of(2023, 1, 1),
            fechaCierre);
    }
}
