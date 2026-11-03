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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que el endpoint de cuentas no se expone con el perfil de
 * producción, donde la propiedad de exposición vale {@code false}.
 */
@WebMvcTest(CuentaFinancieraController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("prod")
class CuentaFinancieraApiNoExpuestaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @Test
    void elControladorDeCuentasNoSeRegistraEnProduccion() {
        assertThat(context.getBeanNamesForType(CuentaFinancieraController.class))
            .isEmpty();
    }

    @Test
    void elEndpointResponde404EnProduccion() throws Exception {
        mockMvc.perform(get(
                "/api/v1/portafolios/{portafolioId}/cuentas",
                UUID.fromString("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isNotFound());
    }
}
