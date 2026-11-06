package com.patrimonio.acceso.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
 * Pruebas del catálogo de monedas y del alta de portafolios autorizados.
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
    private static final String RUTA_MONEDAS = "/api/v1/monedas";
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
        monedaId = crearMoneda("USD", "Dólar estadounidense", "$", true);
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
        UUID autorizado = crearPortafolio(
            "Portafolio autorizado", "Fondo de emergencia", true);
        UUID ajeno = crearPortafolio("Portafolio ajeno", null, true);
        UUID inactivo = crearPortafolio("Portafolio inactivo", null, false);
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
            .andExpect(jsonPath("$[0].descripcion").value("Fondo de emergencia"))
            .andExpect(jsonPath("$[0].monedaBaseId").value(monedaId.toString()))
            .andExpect(jsonPath("$[0].monedaBase.id").value(monedaId.toString()))
            .andExpect(jsonPath("$[0].monedaBase.codigo").value("USD"))
            .andExpect(jsonPath("$[0].monedaBase.nombre")
                .value("Dólar estadounidense"))
            .andExpect(jsonPath("$[0].monedaBase.simbolo").value("$"))
            .andExpect(jsonPath("$[0].monedaBase.decimales").value(2))
            .andExpect(jsonPath("$[0].activo").value(true))
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

    @Test
    void crear_creaPortafolioYMembresiaPropietarioYApareceEnElListado() throws Exception {
        UUID usuarioId = crearUsuario("titular@example.com", true);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        MvcResult resultado = mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nombre": "  Viaje 2027  ",
                      "descripcion": "  Fondo para el viaje  ",
                      "monedaBaseId": "%s"
                    }
                    """.formatted(monedaId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location",
                org.hamcrest.Matchers.startsWith(RUTA + "/")))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.nombre").value("Viaje 2027"))
            .andExpect(jsonPath("$.descripcion").value("Fondo para el viaje"))
            .andExpect(jsonPath("$.monedaBaseId").value(monedaId.toString()))
            .andExpect(jsonPath("$.monedaBase.codigo").value("USD"))
            .andExpect(jsonPath("$.activo").value(true))
            .andReturn();

        String creadoId = JsonPath.read(
            resultado.getResponse().getContentAsString(), "$.id");

        Integer membresias = jdbcTemplate.queryForObject("""
            select count(*)
            from membresia
            where usuario_id = ? and portafolio_id = ? and rol = 'PROPIETARIO'
            """, Integer.class, usuarioId, UUID.fromString(creadoId));
        assertThat(membresias).isEqualTo(1);

        mockMvc.perform(get(RUTA).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(creadoId))
            .andExpect(jsonPath("$[0].nombre").value("Viaje 2027"));

        mockMvc.perform(get(RUTA + "/" + creadoId + "/cuentas").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void crear_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(post(RUTA)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Viaje","monedaBaseId":"%s"}
                    """.formatted(monedaId)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void crear_sinCsrf_devuelve403() throws Exception {
        crearUsuario("titular@example.com", true);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Viaje","monedaBaseId":"%s"}
                    """.formatted(monedaId)))
            .andExpect(status().isForbidden());
    }

    @Test
    void crear_conNombreVacio_devuelve400() throws Exception {
        crearUsuario("titular@example.com", true);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"   ","monedaBaseId":"%s"}
                    """.formatted(monedaId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));
    }

    @Test
    void crear_conDescripcionDemasiadoLarga_devuelve400() throws Exception {
        crearUsuario("titular@example.com", true);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Viaje","descripcion":"%s","monedaBaseId":"%s"}
                    """.formatted("a".repeat(501), monedaId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDACION_ENTRADA"));
    }

    @Test
    void crear_conMonedaInexistente_devuelve422() throws Exception {
        crearUsuario("titular@example.com", true);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Viaje","monedaBaseId":"%s"}
                    """.formatted(UUID.randomUUID())))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("MONEDA_INVALIDA"));
    }

    @Test
    void crear_conMonedaInactiva_devuelve422() throws Exception {
        crearUsuario("titular@example.com", true);
        UUID inactiva = crearMoneda("GBP", "Libra esterlina", "£", false);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(post(RUTA)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nombre":"Viaje","monedaBaseId":"%s"}
                    """.formatted(inactiva)))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("MONEDA_INVALIDA"));
    }

    @Test
    void monedas_listaSoloLasActivasOrdenadasPorCodigo() throws Exception {
        crearUsuario("titular@example.com", true);
        crearMoneda("EUR", "Euro", "€", true);
        crearMoneda("GBP", "Libra esterlina", "£", false);
        MockHttpSession sesion = iniciarSesion("titular@example.com");

        mockMvc.perform(get(RUTA_MONEDAS).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].codigo").value("EUR"))
            .andExpect(jsonPath("$[0].nombre").value("Euro"))
            .andExpect(jsonPath("$[0].simbolo").value("€"))
            .andExpect(jsonPath("$[1].codigo").value("USD"));
    }

    @Test
    void monedas_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(RUTA_MONEDAS))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
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

    private UUID crearMoneda(
        String codigo,
        String nombre,
        String simbolo,
        boolean activo
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into moneda (id, codigo, nombre, simbolo, decimales, activo)
            values (?, ?, ?, ?, ?, ?)
            """, id, codigo, nombre, simbolo, 2, activo);
        return id;
    }

    private UUID crearPortafolio(
        String nombre,
        String descripcion,
        boolean activo
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into portafolio
                (id, nombre, moneda_base_id, descripcion, activo)
            values (?, ?, ?, ?, ?)
            """, id, nombre, monedaId, descripcion, activo);
        return id;
    }

    private void crearMembresia(UUID usuarioId, UUID portafolioId) {
        jdbcTemplate.update("""
            insert into membresia (id, usuario_id, portafolio_id, rol)
            values (?, ?, ?, ?)
            """, UUID.randomUUID(), usuarioId, portafolioId, "LECTOR");
    }
}
