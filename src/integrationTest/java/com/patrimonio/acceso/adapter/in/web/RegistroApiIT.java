package com.patrimonio.acceso.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de extremo a extremo del registro de usuarios.
 *
 * <p>Usan PostgreSQL 16 efímero de Testcontainers. Comprueban el contrato
 * HTTP, el hash de la contraseña, la normalización del correo, el duplicado y
 * que el registro no crea una sesión.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RegistroApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA_REGISTRO = "/api/auth/registro";
    private static final String RUTA_LOGIN = "/api/auth/login";
    private static final String RUTA_ME = "/api/auth/me";
    private static final String NOMBRE = "María Pérez";
    private static final String EMAIL = "maria@example.com";
    private static final String PASSWORD = "Secreta123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbcTemplate.update("delete from membresia");
        jdbcTemplate.update("delete from usuario");
    }

    @Test
    void registro_valido_devuelve201SinSesionYGuardaElHash() throws Exception {
        MvcResult resultado = registrar(NOMBRE, EMAIL, PASSWORD)
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.nombre").value(NOMBRE))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.csrfToken").doesNotExist())
            .andReturn();

        assertThat(resultado.getRequest().getSession(false)).isNull();

        String hash = jdbcTemplate.queryForObject(
            "select password_hash from usuario where email = ?",
            String.class,
            EMAIL);

        assertThat(hash)
            .isNotNull()
            .startsWith("$2")
            .isNotEqualTo(PASSWORD);
        assertThat(new BCryptPasswordEncoder().matches(PASSWORD, hash)).isTrue();
    }

    @Test
    void registro_normalizaElCorreoAntesDePersistir() throws Exception {
        registrar(NOMBRE, "  MARIA@EXAMPLE.COM  ", PASSWORD)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(EMAIL));

        Integer cantidad = jdbcTemplate.queryForObject(
            "select count(*) from usuario where email = ?",
            Integer.class,
            EMAIL);

        assertThat(cantidad).isEqualTo(1);
    }

    @Test
    void registro_conCorreoDuplicado_devuelve409SinEchoDelCorreo() throws Exception {
        registrar(NOMBRE, EMAIL, PASSWORD)
            .andExpect(status().isCreated());

        registrar("Otra persona", "MARIA@EXAMPLE.COM", "OtraClave123!")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("EMAIL_DUPLICADO"))
            .andExpect(content().string(not(containsString(EMAIL))));
    }

    @Test
    void registro_conContrasenaCorta_devuelve400YNoPersiste() throws Exception {
        registrar(NOMBRE, EMAIL, "corta")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));

        Integer cantidad = jdbcTemplate.queryForObject(
            "select count(*) from usuario", Integer.class);

        assertThat(cantidad).isZero();
    }

    @Test
    void registro_conCorreoInvalido_devuelve400() throws Exception {
        registrar(NOMBRE, "no-es-un-correo", PASSWORD)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));
    }

    @Test
    void registro_sinTokenCsrf_devuelve403YNoPersiste() throws Exception {
        mockMvc.perform(post(RUTA_REGISTRO)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(NOMBRE, EMAIL, PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));

        Integer cantidad = jdbcTemplate.queryForObject(
            "select count(*) from usuario", Integer.class);

        assertThat(cantidad).isZero();
    }

    @Test
    void registro_conJsonMalFormado_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA_REGISTRO)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{no-es-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));
    }

    @Test
    void registro_noIniciaSesion() throws Exception {
        registrar(NOMBRE, EMAIL, PASSWORD)
            .andExpect(status().isCreated());

        mockMvc.perform(get(RUTA_ME))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void trasElRegistro_elLoginConLasMismasCredencialesFunciona() throws Exception {
        registrar(NOMBRE, EMAIL, PASSWORD)
            .andExpect(status().isCreated());

        mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuario":"%s","password":"%s"}
                    """.formatted(EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mecanismo").value("SESSION"))
            .andExpect(jsonPath("$.usuario.email").value(EMAIL));
    }

    private org.springframework.test.web.servlet.ResultActions registrar(
        String nombre,
        String email,
        String password
    ) throws Exception {
        return mockMvc.perform(post(RUTA_REGISTRO)
            .with(CsrfTestSupport.conCsrf(mockMvc))
            .contentType(MediaType.APPLICATION_JSON)
            .content(cuerpo(nombre, email, password)));
    }

    private static String cuerpo(String nombre, String email, String password) {
        return """
            {"nombre":"%s","email":"%s","password":"%s"}
            """.formatted(nombre, email, password);
    }
}
