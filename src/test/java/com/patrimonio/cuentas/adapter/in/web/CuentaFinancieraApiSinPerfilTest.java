package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regresión del riesgo residual: sin perfil explícito el endpoint no debe
 * registrarse, aunque Spring aplique {@code spring.profiles.default=local} y la
 * propiedad de exposición valga {@code true}.
 *
 * <p>La prueba fuerza {@code patrimonio.api.cuentas.expuesta=true} para
 * demostrar que la protección no depende solo del valor por defecto de la
 * propiedad.</p>
 */
@WebMvcTest(CuentaFinancieraController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "patrimonio.api.cuentas.expuesta=true")
class CuentaFinancieraApiSinPerfilTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void sinPerfilExplicitoNoHayPerfilesActivos() {
        assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
    }

    @Test
    void sinPerfilExplicitoElControladorNoSeRegistra() {
        assertThat(context.getBeanNamesForType(CuentaFinancieraController.class))
            .isEmpty();
    }

    @Test
    void sinPerfilExplicitoElEndpointResponde404() throws Exception {
        mockMvc.perform(get(
                "/api/v1/portafolios/{portafolioId}/cuentas",
                UUID.fromString("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isNotFound());
    }
}
