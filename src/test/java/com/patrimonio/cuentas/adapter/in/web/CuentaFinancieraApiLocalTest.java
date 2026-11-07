package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.cuentas.application.port.in.ActualizarCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.CambiarEstadoCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.ConsultarCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.CrearCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.testsupport.PrincipalAuthenticationTestConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Con el perfil {@code local} activo de forma explícita y la propiedad de
 * exposición habilitada, el controlador se registra y atiende peticiones.
 */
@WebMvcTest(CuentaFinancieraController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
@TestPropertySource(properties = "patrimonio.api.cuentas.expuesta=true")
@Import({CuentaFinancieraResponseAssembler.class, PrincipalAuthenticationTestConfig.class})
class CuentaFinancieraApiLocalTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PORTAFOLIO_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private ListarCuentasPorPortafolioUseCase listarCuentas;

    @MockitoBean
    private ConsultarCuentaUseCase consultarCuenta;

    @MockitoBean
    private CrearCuentaUseCase crearCuenta;

    @MockitoBean
    private ActualizarCuentaUseCase actualizarCuenta;

    @MockitoBean
    private CambiarEstadoCuentaUseCase cambiarEstadoCuenta;

    @BeforeEach
    void establecerContextoDeSeguridad() {
        SecurityContextHolder.getContext().setAuthentication(autenticacion());
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void conPerfilLocalExplicitoElControladorSeRegistra() {
        assertThat(context.getEnvironment().getActiveProfiles())
            .contains("local");
        assertThat(context.getBeanNamesForType(CuentaFinancieraController.class))
            .hasSize(1);
    }

    @Test
    void conPerfilLocalExplicitoElEndpointResponde200() throws Exception {
        when(listarCuentas.execute(
                new ListarCuentasPorPortafolioQuery(USUARIO_ID, PORTAFOLIO_ID)))
            .thenReturn(List.of());

        mockMvc.perform(get(
                "/api/v1/portafolios/{portafolioId}/cuentas", PORTAFOLIO_ID)
                .with(authentication(autenticacion())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    private static UsernamePasswordAuthenticationToken autenticacion() {
        return UsernamePasswordAuthenticationToken.authenticated(
            new UsuarioAutenticado(
                USUARIO_ID, "usuario@example.com", "Usuario de prueba"),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USUARIO")));
    }
}
