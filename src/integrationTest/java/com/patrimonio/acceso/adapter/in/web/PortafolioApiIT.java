package com.patrimonio.acceso.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas del endpoint de portafolios autorizados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PortafolioApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA = "/api/v1/portafolios";
    private static final String PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID monedaId;

    @BeforeEach
    void prepararMoneda() {
        monedaId = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into moneda (id, codigo, nombre, decimales) values (?, ?, ?, ?)",
            monedaId, "USD", "Dólar estadounidense", 2);
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbcTemplate.update("delete from membresia");
        jdbcTemplate.update("delete from portafolio");
        jdbcTemplate.update("delete from usuario");
        jdbcTemplate.update("delete from moneda");
    }

    @Test
    void listar_devuelveSoloLosPortafoliosActivosConMembresia() throws Exception {
        UUID usuarioId = crearUsuario("titular@example.com", true);
        UUID autorizado = crearPortafolio("Portafolio autorizado", true);
        UUID ajeno = crearPortafolio("Portafolio ajeno", true);
        UUID inactivo = crearPortafolio("Portafolio inactivo", false);
        crearMembresia(usuarioId, autorizado);
        crearMembresia(usuarioId, inactivo);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(get(RUTA).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(autorizado.toString()))
            .andExpect(jsonPath("$[0].nombre").value("Portafolio autorizado"))
            .andExpect(jsonPath("$[0].monedaBaseId").value(monedaId.toString()))
            .andExpect(content().string(
                org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.containsString(ajeno.toString()))));
    }

    @Test
    void listar_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void listar_cuandoElUsuarioNoTieneMembresias_devuelveListaVacia() throws Exception {
        crearUsuario("sin-acceso@example.com", true);
        MockHttpSession sesion = iniciarSesion("sin-acceso@example.com");

        mockMvc.perform(get(RUTA).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
    }

    private MockHttpSession iniciarSesion(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuario":"%s","password":"%s"}
                    """.formatted(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private UUID crearUsuario(String email, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, ?)
            """, id, "Usuario de prueba", email, PASSWORD_HASH, activo);
        return id;
    }

    private UUID crearPortafolio(String nombre, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into portafolio (id, nombre, moneda_base_id, activo)
            values (?, ?, ?, ?)
            """, id, nombre, monedaId, activo);
        return id;
    }

    private void crearMembresia(UUID usuarioId, UUID portafolioId) {
        jdbcTemplate.update("""
            insert into membresia (id, usuario_id, portafolio_id, rol)
            values (?, ?, ?, ?)
            """, UUID.randomUUID(), usuarioId, portafolioId, "LECTOR");
    }
}
