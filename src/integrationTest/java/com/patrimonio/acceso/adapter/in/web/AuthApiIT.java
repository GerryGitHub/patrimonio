package com.patrimonio.acceso.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.patrimonio.acceso.adapter.in.security.SesionExpiracionFilter;
import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
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
 * Pruebas de extremo a extremo de autenticación, sesión, revocación y CSRF.
 *
 * <p>Usan PostgreSQL 16 efímero de Testcontainers y datos ficticios. No se
 * conectan a la base local ni reutilizan sus datos.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String EMAIL = "titular@example.com";
    private static final String PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(PASSWORD);
    private static final String RUTA_LOGIN = "/api/auth/login";
    private static final String RUTA_ME = "/api/auth/me";
    private static final String RUTA_CSRF = "/api/auth/csrf";
    private static final String RUTA_LOGOUT = "/api/auth/logout";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID usuarioId;

    @BeforeEach
    void prepararUsuario() {
        usuarioId = crearUsuario(EMAIL, true);
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbcTemplate.update("delete from membresia");
        jdbcTemplate.update("delete from usuario");
        jdbcTemplate.update("delete from portafolio");
    }

    @Test
    void login_conCredencialesValidas_devuelve200SinExponerLaContrasena() throws Exception {
        MvcResult resultado = login();

        assertThat(resultado.getRequest().getSession(false)).isNotNull();
        assertThat(resultado.getResponse().getContentAsString())
            .doesNotContain(PASSWORD)
            .doesNotContain(PASSWORD_HASH)
            .doesNotContain("$2a$");

        mockMvc.perform(get(RUTA_ME).session(sesion(resultado)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(usuarioId.toString()))
            .andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.nombre").value("Usuario de prueba"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void laContrasenaSeAlmacenaComoHashYNoComoTextoPlano() {
        String hash = jdbcTemplate.queryForObject(
            "select password_hash from usuario where id = ?",
            String.class,
            usuarioId);

        assertThat(hash)
            .isNotNull()
            .startsWith("$2")
            .isNotEqualTo(PASSWORD);
    }

    @Test
    void login_conContrasenaIncorrecta_devuelve401Generico() throws Exception {
        mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(EMAIL, "incorrecta")))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"))
            .andExpect(jsonPath("$.detail").value("Credenciales inválidas."))
            .andExpect(content().string(not(containsString("incorrecta"))));
    }

    @Test
    void login_conUsuarioInexistente_respondeIgualQueConContrasenaIncorrecta()
        throws Exception {
        MvcResult inexistente = mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("nadie@example.com", PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andReturn();

        MvcResult incorrecta = mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(EMAIL, "incorrecta")))
            .andExpect(status().isUnauthorized())
            .andReturn();

        String cuerpoInexistente = inexistente.getResponse().getContentAsString();
        String cuerpoIncorrecta = incorrecta.getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(cuerpoInexistente, "$.code"))
            .isEqualTo(JsonPath.<String>read(cuerpoIncorrecta, "$.code"));
        assertThat(JsonPath.<String>read(cuerpoInexistente, "$.detail"))
            .isEqualTo(JsonPath.<String>read(cuerpoIncorrecta, "$.detail"));
        assertThat(cuerpoInexistente).doesNotContain("nadie@example.com");
    }

    @Test
    void login_conUsuarioInactivo_devuelve401() throws Exception {
        jdbcTemplate.update("update usuario set activo = false where id = ?", usuarioId);

        mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(EMAIL, PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void me_sinSesion_devuelve401ProblemDetail() throws Exception {
        mockMvc.perform(get(RUTA_ME))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"))
            .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void logout_invalidaLaSesionYDespuesMeRechazaLaCredencial() throws Exception {
        MvcResult login = login();
        MockHttpSession sesion = sesion(login);

        mockMvc.perform(post(RUTA_LOGOUT).session(sesion).with(CsrfTestSupport.conCsrf(mockMvc)))
            .andExpect(status().isNoContent());

        assertThat(sesion.isInvalid()).isTrue();

        mockMvc.perform(get(RUTA_ME).session(new MockHttpSession()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_sinSesion_esIdempotente() throws Exception {
        mockMvc.perform(post(RUTA_LOGOUT).with(CsrfTestSupport.conCsrf(mockMvc)))
            .andExpect(status().isNoContent());
    }

    @Test
    void sesion_cuandoSuperaLaDuracionMaxima_devuelve401() throws Exception {
        MvcResult login = login();
        MockHttpSession sesion = sesion(login);
        sesion.setAttribute(
            SesionExpiracionFilter.ATRIBUTO_INICIO_SESION,
            Instant.now().minus(Duration.ofHours(9)));

        mockMvc.perform(get(RUTA_ME).session(sesion))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void login_sinTokenCsrf_devuelve403YNoAutentica() throws Exception {
        mockMvc.perform(post(RUTA_LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(EMAIL, PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void csrf_entregaElTokenYLoginLoRota() throws Exception {
        MvcResult csrf = mockMvc.perform(get(RUTA_CSRF))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.headerName").value(CsrfTestSupport.CABECERA))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn();

        String tokenInicial = JsonPath.read(
            csrf.getResponse().getContentAsString(), "$.token");
        Cookie cookie = csrf.getResponse().getCookie(CsrfTestSupport.COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isFalse();

        var peticion = post(RUTA_LOGIN);

        MvcResult login = mockMvc.perform(peticion
                .cookie(cookie)
                .header(CsrfTestSupport.CABECERA, tokenInicial)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        String tokenRotado = JsonPath.read(
            login.getResponse().getContentAsString(), "$.csrfToken");
        assertThat(tokenRotado).isNotBlank().isNotEqualTo(tokenInicial);
    }

    @Test
    void login_conCuerpoInvalido_devuelve400SinEchoDeValores() throws Exception {
        mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usuario\":\"\",\"password\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"))
            .andExpect(content().string(not(containsString("usuario"))));
    }

    @Test
    void login_conJsonMalFormado_devuelve400() throws Exception {
        mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{no-es-json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post(RUTA_LOGIN)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(EMAIL, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mecanismo").value("SESSION"))
            .andExpect(jsonPath("$.usuario.email").value(EMAIL))
            .andExpect(jsonPath("$.expiraEnSegundos").isNumber())
            .andExpect(jsonPath("$.duracionMaximaSegundos").isNumber())
            .andExpect(jsonPath("$.csrfToken").isNotEmpty())
            .andReturn();
    }

    private static MockHttpSession sesion(MvcResult resultado) {
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }

    private static String cuerpo(String usuario, String password) {
        return """
            {"usuario":"%s","password":"%s"}
            """.formatted(usuario, password);
    }

    private UUID crearUsuario(String email, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, ?)
            """, id, "Usuario de prueba", email, PASSWORD_HASH, activo);
        return id;
    }
}
