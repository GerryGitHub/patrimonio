package com.patrimonio.cuentas.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.NotFoundException;
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
 * Pruebas del caso de uso sin Spring, sin base de datos y sin Docker.
 */
@ExtendWith(MockitoExtension.class)
class ListarCuentasPorPortafolioServiceTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PORTAFOLIO_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INSTITUCION_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO_CUENTA_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MONEDA_ID =
        UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private AccesoQueryPort accesoQueryPort;

    @Mock
    private CuentaFinancieraRepository cuentaFinancieraRepository;

    private ListarCuentasPorPortafolioService service;

    @BeforeEach
    void setUp() {
        service = new ListarCuentasPorPortafolioService(
            accesoQueryPort, cuentaFinancieraRepository);
    }

    @Test
    void execute_cuandoElUsuarioTieneMembresia_devuelveLasCuentasDelPuerto() {
        CuentaFinanciera cuentaCorriente = cuenta(
            "11111111-aaaa-1111-1111-111111111111",
            "Cuenta corriente",
            new NumeroCuenta("ES91 2100 0418 4502 0005 1332"),
            true,
            LocalDate.of(2024, 1, 15),
            null);
        CuentaFinanciera cuentaAhorro = cuenta(
            "22222222-bbbb-2222-2222-222222222222",
            "Cuenta de ahorro",
            null,
            true,
            null,
            null);

        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(new PermisoPortafolio(PORTAFOLIO_ID, true)));
        when(cuentaFinancieraRepository.listarPorPortafolio(PORTAFOLIO_ID))
            .thenReturn(List.of(cuentaCorriente, cuentaAhorro));

        List<CuentaFinancieraView> resultado = service.execute(consulta());

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).id()).isEqualTo(cuentaCorriente.id());
        assertThat(resultado.get(0).nombre()).isEqualTo("Cuenta corriente");
        assertThat(resultado.get(0).numeroCuenta()).isEqualTo("ES91 2100 0418 4502 0005 1332");
        assertThat(resultado.get(0).fechaApertura()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(resultado.get(1).numeroCuenta()).isNull();
        assertThat(resultado.get(1).fechaApertura()).isNull();
        assertThat(resultado.get(1).fechaCierre()).isNull();

        verify(cuentaFinancieraRepository).listarPorPortafolio(PORTAFOLIO_ID);
    }

    @Test
    void execute_cuandoNoHayCuentas_devuelveListaVacia() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(new PermisoPortafolio(PORTAFOLIO_ID, true)));
        when(cuentaFinancieraRepository.listarPorPortafolio(PORTAFOLIO_ID))
            .thenReturn(List.of());

        List<CuentaFinancieraView> resultado = service.execute(consulta());

        assertThat(resultado).isEmpty();
    }

    @Test
    void execute_cuandoNoHayMembresia_lanzaNotFoundSinConsultarCuentas() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(consulta()))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("portafolio");

        verifyNoInteractions(cuentaFinancieraRepository);
    }

    @Test
    void execute_cuandoLaMembresiaNoConcedeLectura_lanzaAccesoDenegado() {
        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(new PermisoPortafolio(PORTAFOLIO_ID, false)));

        assertThatThrownBy(() -> service.execute(consulta()))
            .isInstanceOfSatisfying(
                AccesoDenegadoException.class,
                ex -> assertThat(ex.codigo()).isEqualTo("ACCESO_DENEGADO"));

        verifyNoInteractions(cuentaFinancieraRepository);
    }

    @Test
    void execute_cuandoElPuertoDeCuentasFalla_propagaLaExcepcionDelContrato() {
        DatosPersistidosInvalidos fallo = new DatosPersistidosInvalidos("dato corrupto");

        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenReturn(Optional.of(new PermisoPortafolio(PORTAFOLIO_ID, true)));
        when(cuentaFinancieraRepository.listarPorPortafolio(PORTAFOLIO_ID))
            .thenThrow(fallo);

        assertThatThrownBy(() -> service.execute(consulta()))
            .isSameAs(fallo);
    }

    @Test
    void execute_cuandoElPuertoDeAccesoFalla_propagaLaExcepcion() {
        IllegalStateException fallo = new IllegalStateException("base no disponible");

        when(accesoQueryPort.buscarPermiso(USUARIO_ID, PORTAFOLIO_ID))
            .thenThrow(fallo);

        assertThatThrownBy(() -> service.execute(consulta()))
            .isSameAs(fallo);

        verifyNoInteractions(cuentaFinancieraRepository);
    }

    @Test
    void execute_cuandoLaConsultaEsNula_falla() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(NullPointerException.class);
    }

    private static ListarCuentasPorPortafolioQuery consulta() {
        return new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID);
    }

    private static CuentaFinanciera cuenta(
        String id,
        String nombre,
        NumeroCuenta numeroCuenta,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        return CuentaFinanciera.reconstruir(
            UUID.fromString(id),
            PORTAFOLIO_ID,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            nombre,
            numeroCuenta,
            MONEDA_ID,
            activo,
            fechaApertura,
            fechaCierre);
    }
}
