package com.patrimonio.cuentas.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.NotFoundException;
import com.patrimonio.testsupport.PrincipalAuthenticationTestConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas del contrato HTTP del controlador, sin base de datos ni Docker.
 */
@WebMvcTest(CuentaFinancieraController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({CuentaFinancieraResponseAssembler.class, PrincipalAuthenticationTestConfig.class})
class CuentaFinancieraControllerTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PORTAFOLIO_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUENTA_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INSTITUCION_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO_CUENTA_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MONEDA_ID =
        UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String RUTA = "/api/v1/portafolios/{portafolioId}/cuentas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListarCuentasPorPortafolioUseCase listarCuentas;

    @BeforeEach
    void establecerContextoDeSeguridad() {
        SecurityContextHolder.getContext().setAuthentication(autenticacion());
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listar_cuandoHayCuentas_devuelve200YContratoJsonConNumeroEnmascarado()
        throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenReturn(List.of(cuentaView("ES91 2100 0418 4502 0005 1332")));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(CUENTA_ID.toString()))
            .andExpect(jsonPath("$[0].nombre").value("Cuenta corriente"))
            .andExpect(jsonPath("$[0].numeroCuenta").value("****1332"))
            .andExpect(jsonPath("$[0].institucionId").value(INSTITUCION_ID.toString()))
            .andExpect(jsonPath("$[0].tipoCuentaId").value(TIPO_CUENTA_ID.toString()))
            .andExpect(jsonPath("$[0].monedaId").value(MONEDA_ID.toString()))
            .andExpect(jsonPath("$[0].activo").value(true))
            .andExpect(jsonPath("$[0].fechaApertura").value("2024-01-15"))
            .andExpect(jsonPath("$[0].fechaCierre").value(nullValue()))
            .andExpect(jsonPath("$[0].portafolioId").doesNotExist())
            .andExpect(jsonPath("$[0].password").doesNotExist())
            .andExpect(content().string(not(containsString("ES91 2100"))));

        verify(listarCuentas).execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID));
    }

    @Test
    void listar_cuandoElNumeroEsCorto_loEnmascaraCompletamente() throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenReturn(List.of(cuentaView("123")));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numeroCuenta").value("***"))
            .andExpect(content().string(not(containsString("123"))));
    }

    @Test
    void listar_cuandoElNumeroEsNulo_loDevuelveNuloSinFallar() throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenReturn(List.of(cuentaView(null)));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].numeroCuenta").value(nullValue()));
    }

    @Test
    void listar_cuandoNoHayCuentas_devuelve200YArrayVacio() throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenReturn(List.of());

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listar_cuandoPortafolioNoExiste_devuelve404ProblemDetail() throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenThrow(new NotFoundException("No existe el portafolio indicado."));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
            .andExpect(jsonPath("$.detail").value("No existe el portafolio indicado."));
    }

    @Test
    void listar_cuandoElIdentificadorNoEsUuid_devuelve400ProblemDetail() throws Exception {
        mockMvc.perform(get(RUTA, "no-es-un-uuid").with(authentication(autenticacion())))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("ID_INVALIDO"))
            .andExpect(jsonPath("$.detail").value(containsString("portafolioId")))
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(content().string(not(containsString("MethodArgumentTypeMismatch"))));
    }

    @Test
    void listar_cuandoElDominioRechazaLaConsulta_devuelve422ProblemDetail()
        throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenThrow(new CuentaInvalidaException(
                "La fecha de cierre no puede ser anterior a la fecha de apertura."));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.code").value("CUENTA_INVALIDA"))
            .andExpect(jsonPath("$.detail").value(
                "La fecha de cierre no puede ser anterior a la fecha de apertura."));
    }

    @Test
    void listar_cuandoLosDatosSonInconsistentes_devuelve500SinDetalleInterno()
        throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenThrow(new DatosPersistidosInvalidos("detalle interno que no debe salir"));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("DATOS_INCONSISTENTES"))
            .andExpect(jsonPath("$.detail").value("Los datos almacenados no son coherentes."))
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(content().string(not(containsString("detalle interno"))));
    }

    @Test
    void listar_cuandoOcurreUnErrorInesperado_devuelve500SinDetalleInterno()
        throws Exception {
        when(listarCuentas.execute(new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenThrow(new IllegalStateException("detalle interno que no debe salir"));

        mockMvc.perform(get(RUTA, PORTAFOLIO_ID).with(authentication(autenticacion())))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
            .andExpect(jsonPath("$.detail").value("Se produjo un error interno."))
            .andExpect(jsonPath("$.trace").doesNotExist())
            .andExpect(jsonPath("$.exception").doesNotExist())
            .andExpect(content().string(not(containsString("detalle interno"))));
    }

    private static UsernamePasswordAuthenticationToken autenticacion() {
        return UsernamePasswordAuthenticationToken.authenticated(
            new UsuarioAutenticado(
                USUARIO_ID, "usuario@example.com", "Usuario de prueba"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    private static CuentaFinancieraView cuentaView(String numeroCuenta) {
        return new CuentaFinancieraView(
            CUENTA_ID,
            "Cuenta corriente",
            numeroCuenta,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            MONEDA_ID,
            true,
            LocalDate.of(2024, 1, 15),
            null);
    }
}
