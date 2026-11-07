package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Pruebas de extremo a extremo del catálogo de instituciones contra un
 * PostgreSQL 16 efímero.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
class InstitucionApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA = "/api/v1/instituciones";
    private static final String USUARIO_EMAIL = "instituciones@example.com";
    private static final String USUARIO_PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(USUARIO_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararUsuario() {
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, ?)
            """, UUID.randomUUID(), "Usuario de prueba", USUARIO_EMAIL,
            PASSWORD_HASH, true);
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbcTemplate.update("delete from cuenta_financiera");
        jdbcTemplate.update("delete from membresia");
        jdbcTemplate.update("delete from portafolio");
        jdbcTemplate.update("delete from usuario");
        jdbcTemplate.update("delete from institucion");
    }

    @Test
    void listar_sinInstituciones_devuelveArrayVacio() throws Exception {
        mockMvc.perform(get(RUTA).session(iniciarSesion()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void crear_devuelve201ConLocationYLaInstitucion() throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "  Banco   del Norte ",
                      "tipo": "BANCO",
                      "pais": "México",
                      "sitioWeb": "https://banco.example"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString(RUTA + "/")))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.nombre").value("Banco del Norte"))
            .andExpect(jsonPath("$.tipo").value("BANCO"))
            .andExpect(jsonPath("$.pais").value("México"))
            .andExpect(jsonPath("$.sitioWeb").value("https://banco.example"))
            .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get(RUTA).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Banco del Norte"));
    }

    @Test
    void crear_conNombreDuplicadoIgnorandoMayusculas_devuelve409() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        crearInstitucion(sesion, "Banco del Norte", "BANCO");

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre": "banco del norte", "tipo": "BANCO"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("INSTITUCION_DUPLICADA"));
    }

    @Test
    void crear_conCategoriaInvalida_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA)
                .session(iniciarSesion())
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre": "Institución rara", "tipo": "NO_EXISTE"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));
    }

    @Test
    void consultar_cuandoNoExiste_devuelve404() throws Exception {
        mockMvc.perform(get(RUTA + "/" + UUID.randomUUID()).session(iniciarSesion()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    void listar_filtraPorCategoriaYEstado() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        crearInstitucion(sesion, "Banco Uno", "BANCO");
        crearInstitucion(sesion, "SOFIPO Uno", "SOFIPO");
        UUID inactiva = crearInstitucion(sesion, "Exchange Uno", "EXCHANGE");
        jdbcTemplate.update(
            "update institucion set activo = false where id = ?", inactiva);

        mockMvc.perform(get(RUTA).param("tipo", "SOFIPO").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("SOFIPO Uno"));

        mockMvc.perform(get(RUTA).param("activo", "false").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Exchange Uno"));
    }

    @Test
    void listar_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(RUTA))
            .andExpect(status().isUnauthorized())
            .andExpect(content().string(not(containsString("Banco"))));
    }

    private UUID crearInstitucion(
        MockHttpSession sesion,
        String nombre,
        String tipo
    ) throws Exception {
        MvcResult resultado = mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre": "%s", "tipo": "%s"}
                    """.formatted(nombre, tipo)))
            .andExpect(status().isCreated())
            .andReturn();

        String id = com.jayway.jsonpath.JsonPath.read(
            resultado.getResponse().getContentAsString(), "$.id");
        return UUID.fromString(id);
    }

    private MockHttpSession iniciarSesion() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuario":"%s","password":"%s"}
                    """.formatted(USUARIO_EMAIL, USUARIO_PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) resultado.getRequest().getSession(false);
    }
}
