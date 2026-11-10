package com.patrimonio.inversiones.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
 * Pruebas de extremo a extremo del catálogo de instrumentos y de las
 * posiciones contra un PostgreSQL 16 efímero.
 *
 * <p>Cubren autenticación, autorización por membresía, aislamiento entre
 * portafolios, validación de compatibilidad y cálculo del costo total. No se
 * mockea ningún puerto y no se usa la base de desarrollo.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class InversionesApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA_INSTRUMENTOS = "/api/v1/instrumentos";
    private static final String RUTA_POSICIONES =
        "/api/v1/portafolios/{portafolioId}/posiciones";
    private static final String USUARIO_EMAIL = "inversiones@example.com";
    private static final String USUARIO_PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(USUARIO_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID monedaId;
    private UUID institucionId;
    private UUID tipoCorretajeId;
    private UUID tipoExchangeId;
    private UUID tipoPlazoId;
    private UUID tipoCorrienteId;
    private UUID claseRentaVariableId;
    private UUID claseRentaFijaId;
    private UUID claseDigitalId;
    private UUID tipoAccionId;
    private UUID tipoBonoId;
    private UUID tipoCriptoId;
    private UUID usuarioId;

    @BeforeEach
    void prepararCatalogos() {
        monedaId = UUID.randomUUID();
        institucionId = UUID.randomUUID();

        jdbcTemplate.update(
            "insert into moneda (id, codigo, nombre, decimales) values (?, ?, ?, ?)",
            monedaId, "USD", "Dólar estadounidense", 2);
        jdbcTemplate.update(
            "insert into institucion (id, nombre) values (?, ?)",
            institucionId, "Casa de bolsa de prueba");

        tipoCorretajeId = insertarTipoCuenta("Cuenta de corretaje");
        tipoExchangeId = insertarTipoCuenta("Cuenta de exchange");
        tipoPlazoId = insertarTipoCuenta("Cuenta a plazo");
        tipoCorrienteId = insertarTipoCuenta("Cuenta corriente");

        claseRentaVariableId = insertarClaseActivo("Renta variable");
        claseRentaFijaId = insertarClaseActivo("Renta fija");
        claseDigitalId = insertarClaseActivo("Activos digitales");

        tipoAccionId = insertarTipoInstrumento(claseRentaVariableId, "Acción");
        tipoBonoId = insertarTipoInstrumento(claseRentaFijaId, "Bono corporativo");
        tipoCriptoId = insertarTipoInstrumento(claseDigitalId, "Criptomoneda");

        usuarioId = crearUsuario(USUARIO_EMAIL, true);
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbcTemplate.update("delete from identificador_instrumento");
        jdbcTemplate.update("delete from posicion");
        jdbcTemplate.update("delete from instrumento");
        jdbcTemplate.update("delete from tipo_instrumento");
        jdbcTemplate.update("delete from clase_activo");
        jdbcTemplate.update("delete from membresia");
        jdbcTemplate.update("delete from cuenta_financiera");
        jdbcTemplate.update("delete from portafolio");
        jdbcTemplate.update("delete from usuario");
        jdbcTemplate.update("delete from institucion");
        jdbcTemplate.update("delete from tipo_cuenta");
        jdbcTemplate.update("delete from moneda");
    }

    @Test
    void catalogo_listaCategoriasYTipos() throws Exception {
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(get(RUTA_INSTRUMENTOS + "/categorias").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].nombre").value("Activos digitales"))
            .andExpect(jsonPath("$[1].nombre").value("Renta fija"))
            .andExpect(jsonPath("$[2].nombre").value("Renta variable"));

        mockMvc.perform(get(RUTA_INSTRUMENTOS + "/tipos")
                .param("claseActivoId", claseRentaVariableId.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Acción"))
            .andExpect(jsonPath("$[0].claseActivoNombre").value("Renta variable"));

        mockMvc.perform(get(RUTA_INSTRUMENTOS + "/tipos").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void crearInstrumento_conDatosValidos_guardaYNormaliza() throws Exception {
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(post(RUTA_INSTRUMENTOS)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoInstrumentoId": "%s",
                      "nombre": "  Apple   Inc. ",
                      "simbolo": " aapl ",
                      "emisor": "Apple",
                      "mercado": "NASDAQ",
                      "monedaId": "%s",
                      "identificadorExterno": "PROV-1",
                      "isin": " us0378331005 ",
                      "descripcion": "Acción de Apple"
                    }
                    """.formatted(tipoAccionId, monedaId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString(
                RUTA_INSTRUMENTOS + "/")))
            .andExpect(jsonPath("$.nombre").value("Apple Inc."))
            .andExpect(jsonPath("$.simbolo").value("AAPL"))
            .andExpect(jsonPath("$.isin").value("US0378331005"))
            .andExpect(jsonPath("$.claseActivoNombre").value("Renta variable"))
            .andExpect(jsonPath("$.tipoInstrumentoNombre").value("Acción"))
            .andExpect(jsonPath("$.monedaCodigo").value("USD"))
            .andExpect(jsonPath("$.activo").value(true))
            .andExpect(content().string(not(containsString("hibernateLazyInitializer"))));

        assertThat(jdbcTemplate.queryForObject(
            "select nombre from instrumento", String.class))
            .isEqualTo("Apple Inc.");
        assertThat(jdbcTemplate.queryForObject(
            "select valor from identificador_instrumento where tipo = 'ISIN'",
            String.class))
            .isEqualTo("US0378331005");
    }

    @Test
    void crearInstrumento_conIsinDuplicado_devuelve409() throws Exception {
        insertarInstrumento("Apple", "AAPL", tipoAccionId, "US0378331005", true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(post(RUTA_INSTRUMENTOS)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoInstrumentoId": "%s",
                      "nombre": "Otra Apple",
                      "simbolo": "AAPL2",
                      "monedaId": "%s",
                      "isin": "US0378331005"
                    }
                    """.formatted(tipoAccionId, monedaId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSTRUMENTO_DUPLICADO"));
    }

    @Test
    void crearInstrumento_conIsinInvalido_devuelve422() throws Exception {
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(post(RUTA_INSTRUMENTOS)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoInstrumentoId": "%s",
                      "nombre": "Apple",
                      "monedaId": "%s",
                      "isin": "ABC"
                    }
                    """.formatted(tipoAccionId, monedaId)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("INSTRUMENTO_INVALIDO"));
    }

    @Test
    void listarInstrumentos_filtraPorBusquedaCategoriaTipoYEstado()
        throws Exception {
        UUID apple = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, "US0378331005", true);
        insertarInstrumento("Bono", "BONO", tipoBonoId, null, true);
        insertarInstrumento("Bitcoin", "BTC", tipoCriptoId, null, false);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(get(RUTA_INSTRUMENTOS)
                .param("busqueda", "apple")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(apple.toString()));

        mockMvc.perform(get(RUTA_INSTRUMENTOS)
                .param("busqueda", "US0378331005")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Apple"));

        mockMvc.perform(get(RUTA_INSTRUMENTOS)
                .param("claseActivoId", claseDigitalId.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Bitcoin"));

        mockMvc.perform(get(RUTA_INSTRUMENTOS)
                .param("tipoInstrumentoId", tipoBonoId.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Bono"));

        mockMvc.perform(get(RUTA_INSTRUMENTOS)
                .param("activo", "false")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Bitcoin"));
    }

    @Test
    void listarInstrumentos_sinFiltrosDevuelveTodoElCatalogo() throws Exception {
        insertarInstrumento("Apple", "AAPL", tipoAccionId, "US0378331005", true);
        insertarInstrumento("Bono", "BONO", tipoBonoId, null, true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(get(RUTA_INSTRUMENTOS).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void consultarYActualizarInstrumento_actualizaElIsin() throws Exception {
        UUID instrumentoId = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, "US0378331005", true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(get(RUTA_INSTRUMENTOS + "/{id}", instrumentoId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Apple"))
            .andExpect(jsonPath("$.isin").value("US0378331005"));

        mockMvc.perform(put(RUTA_INSTRUMENTOS + "/{id}", instrumentoId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoInstrumentoId": "%s",
                      "nombre": "Apple Inc.",
                      "simbolo": "AAPL",
                      "mercado": "NASDAQ",
                      "monedaId": "%s",
                      "isin": "US0378331013"
                    }
                    """.formatted(tipoAccionId, monedaId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Apple Inc."))
            .andExpect(jsonPath("$.isin").value("US0378331013"));

        assertThat(jdbcTemplate.queryForObject(
            "select valor from identificador_instrumento where tipo = 'ISIN' and activo",
            String.class))
            .isEqualTo("US0378331013");

        mockMvc.perform(put(RUTA_INSTRUMENTOS + "/{id}", instrumentoId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipoInstrumentoId": "%s",
                      "nombre": "Apple Inc.",
                      "simbolo": "AAPL",
                      "monedaId": "%s"
                    }
                    """.formatted(tipoAccionId, monedaId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isin").value(nullValue()));

        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from identificador_instrumento where tipo = 'ISIN' and activo",
            Integer.class))
            .isZero();
    }

    @Test
    void cambiarEstadoInstrumento_desactivaYReactiva() throws Exception {
        UUID instrumentoId = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(patch(RUTA_INSTRUMENTOS + "/{id}/estado", instrumentoId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(patch(RUTA_INSTRUMENTOS + "/{id}/estado", instrumentoId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void crearPosicion_conEditor_calculaCostoYListaElResumen() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de inversiones", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta de corretaje", tipoCorretajeId, true);
        UUID instrumento = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, "US0378331005", true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(post(RUTA_POSICIONES, portafolio)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cuentaId": "%s",
                      "instrumentoId": "%s",
                      "cantidad": "10.5",
                      "costoUnitario": "25.75",
                      "fechaApertura": "2026-01-10",
                      "observaciones": "Posición inicial"
                    }
                    """.formatted(cuenta, instrumento)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString(
                "/api/v1/portafolios/" + portafolio + "/posiciones/")))
            .andExpect(jsonPath("$.cuentaId").value(cuenta.toString()))
            .andExpect(jsonPath("$.cuentaNombre").value("Cuenta de corretaje"))
            .andExpect(jsonPath("$.instrumentoNombre").value("Apple"))
            .andExpect(jsonPath("$.instrumentoSimbolo").value("AAPL"))
            .andExpect(jsonPath("$.cantidad").value("10.5"))
            .andExpect(jsonPath("$.costoUnitario").value("25.75"))
            .andExpect(jsonPath("$.costoTotal").value("270.3750000000"))
            .andExpect(jsonPath("$.monedaCodigo").value("USD"))
            .andExpect(jsonPath("$.claseActivoNombre").value("Renta variable"))
            .andExpect(jsonPath("$.fechaApertura").value("2026-01-10"))
            .andExpect(jsonPath("$.activo").value(true));

        mockMvc.perform(get(RUTA_POSICIONES, portafolio).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].costoTotal").value("270.3750000000"));

        mockMvc.perform(get(RUTA_POSICIONES + "/resumen", portafolio)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPosiciones").value(1))
            .andExpect(jsonPath("$.posicionesActivas").value(1))
            .andExpect(jsonPath("$.instrumentosDistintos").value(1))
            .andExpect(jsonPath("$.costoPorMoneda", hasSize(1)))
            .andExpect(jsonPath("$.costoPorMoneda[0].codigo").value("USD"))
            .andExpect(jsonPath("$.costoPorMoneda[0].costo")
                .value("270.3750000000"))
            .andExpect(jsonPath("$.distribucionPorCategoria", hasSize(1)))
            .andExpect(jsonPath("$.distribucionPorCategoria[0].claseActivoNombre")
                .value("Renta variable"))
            .andExpect(jsonPath("$.distribucionPorCategoria[0].posiciones")
                .value(1));
    }

    @Test
    void posiciones_conRolLector_devuelve403() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de solo lectura", true);
        crearMembresia(usuarioId, portafolio, "LECTOR");
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta", tipoCorretajeId, true);
        UUID instrumento = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);

        mockMvc.perform(post(RUTA_POSICIONES, portafolio)
                .session(iniciarSesion(USUARIO_EMAIL))
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cuentaId": "%s",
                      "instrumentoId": "%s",
                      "cantidad": "1",
                      "costoUnitario": "10"
                    }
                    """.formatted(cuenta, instrumento)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void posiciones_conCuentaDeOtroPortafolio_devuelve404() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio propio", true);
        UUID otroPortafolio = crearPortafolio("Portafolio ajeno", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        UUID cuentaAjena = insertarCuenta(
            otroPortafolio, "Cuenta ajena", tipoCorretajeId, true);
        UUID instrumento = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);

        mockMvc.perform(post(RUTA_POSICIONES, portafolio)
                .session(iniciarSesion(USUARIO_EMAIL))
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cuentaId": "%s",
                      "instrumentoId": "%s",
                      "cantidad": "1",
                      "costoUnitario": "10"
                    }
                    """.formatted(cuentaAjena, instrumento)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    void posiciones_conCuentaIncompatible_devuelve422() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio incompatible", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        UUID cuentaExchange = insertarCuenta(
            portafolio, "Exchange", tipoExchangeId, true);
        UUID instrumento = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);

        mockMvc.perform(post(RUTA_POSICIONES, portafolio)
                .session(iniciarSesion(USUARIO_EMAIL))
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cuentaId": "%s",
                      "instrumentoId": "%s",
                      "cantidad": "1",
                      "costoUnitario": "10"
                    }
                    """.formatted(cuentaExchange, instrumento)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("POSICION_INCOMPATIBLE"));
    }

    @Test
    void posiciones_conCuentaAPlazo_devuelve422() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio a plazo", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        UUID cuentaPlazo = insertarCuenta(
            portafolio, "Plazo", tipoPlazoId, true);
        UUID instrumento = insertarInstrumento(
            "Bono", "BONO", tipoBonoId, null, true);

        mockMvc.perform(post(RUTA_POSICIONES, portafolio)
                .session(iniciarSesion(USUARIO_EMAIL))
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cuentaId": "%s",
                      "instrumentoId": "%s",
                      "cantidad": "1",
                      "costoUnitario": "10"
                    }
                    """.formatted(cuentaPlazo, instrumento)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("POSICION_INCOMPATIBLE"));
    }

    @Test
    void posiciones_filtraPorCuentaInstrumentoCategoriaYEstado()
        throws Exception {
        UUID portafolio = crearPortafolio("Portafolio con filtros", true);
        crearMembresia(usuarioId, portafolio, "LECTOR");
        UUID cuentaA = insertarCuenta(
            portafolio, "Cuenta A", tipoCorretajeId, true);
        UUID cuentaB = insertarCuenta(
            portafolio, "Cuenta B", tipoCorretajeId, true);
        UUID apple = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);
        UUID bono = insertarInstrumento(
            "Bono", "BONO", tipoBonoId, null, true);

        insertarPosicion(cuentaA, apple, "10", "20", true);
        insertarPosicion(cuentaB, bono, "5", "100", true);
        insertarPosicion(cuentaA, bono, "2", "50", false);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(get(RUTA_POSICIONES, portafolio)
                .param("cuentaId", cuentaA.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(RUTA_POSICIONES, portafolio)
                .param("instrumentoId", bono.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(RUTA_POSICIONES, portafolio)
                .param("claseActivoId", claseRentaFijaId.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(RUTA_POSICIONES, portafolio)
                .param("activo", "true")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get(RUTA_POSICIONES + "/resumen", portafolio)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPosiciones").value(3))
            .andExpect(jsonPath("$.posicionesActivas").value(2))
            .andExpect(jsonPath("$.instrumentosDistintos").value(2))
            .andExpect(jsonPath("$.distribucionPorCategoria", hasSize(2)));
    }

    @Test
    void posiciones_actualizarYCambiarEstado() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de edición", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta", tipoCorretajeId, true);
        UUID instrumento = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);
        UUID posicion = insertarPosicion(cuenta, instrumento, "10", "20", true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(put(RUTA_POSICIONES + "/{posicionId}", portafolio, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cantidad": "12",
                      "costoUnitario": "21.5",
                      "observaciones": "Corregida"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("12"))
            .andExpect(jsonPath("$.costoTotal").value("258.0000000000"))
            .andExpect(jsonPath("$.observaciones").value("Corregida"));

        mockMvc.perform(patch(
                RUTA_POSICIONES + "/{posicionId}/estado", portafolio, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get(RUTA_POSICIONES, portafolio)
                .param("activo", "false")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void posiciones_noRevelaDatosDeOtroPortafolio() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio propio", true);
        UUID otroPortafolio = crearPortafolio("Portafolio ajeno", true);
        crearMembresia(usuarioId, portafolio, "LECTOR");
        UUID cuentaAjena = insertarCuenta(
            otroPortafolio, "Cuenta ajena", tipoCorretajeId, true);
        UUID instrumento = insertarInstrumento(
            "Apple", "AAPL", tipoAccionId, null, true);
        UUID posicionAjena = insertarPosicion(
            cuentaAjena, instrumento, "10", "20", true);
        MockHttpSession sesion = iniciarSesion(USUARIO_EMAIL);

        mockMvc.perform(get(RUTA_POSICIONES, portafolio).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(
                RUTA_POSICIONES + "/{posicionId}", portafolio, posicionAjena)
                .session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
            .andExpect(content().string(not(containsString("Cuenta ajena"))));
    }

    @Test
    void inversiones_sinSesion_devuelve401() throws Exception {
        mockMvc.perform(get(RUTA_INSTRUMENTOS))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));

        mockMvc.perform(get(RUTA_POSICIONES, UUID.randomUUID()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));
    }

    @Test
    void inversiones_sinMembresia_devuelve404() throws Exception {
        UUID portafolioAjeno = crearPortafolio("Portafolio ajeno", true);

        mockMvc.perform(get(RUTA_POSICIONES, portafolioAjeno)
                .session(iniciarSesion(USUARIO_EMAIL)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    private UUID insertarTipoCuenta(String nombre) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into tipo_cuenta (id, nombre) values (?, ?)", id, nombre);
        return id;
    }

    private UUID insertarClaseActivo(String nombre) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into clase_activo (id, nombre) values (?, ?)", id, nombre);
        return id;
    }

    private UUID insertarTipoInstrumento(UUID claseActivoId, String nombre) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into tipo_instrumento (id, clase_activo_id, nombre)
            values (?, ?, ?)
            """, id, claseActivoId, nombre);
        return id;
    }

    private UUID insertarInstrumento(
        String nombre,
        String simbolo,
        UUID tipoInstrumentoId,
        String isin,
        boolean activo
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into instrumento
                (id, tipo_instrumento_id, nombre, simbolo, moneda_id, activo)
            values (?, ?, ?, ?, ?, ?)
            """, id, tipoInstrumentoId, nombre, simbolo, monedaId, activo);
        if (isin != null) {
            jdbcTemplate.update("""
                insert into identificador_instrumento
                    (id, instrumento_id, tipo, valor, activo)
                values (?, ?, 'ISIN', ?, true)
                """, UUID.randomUUID(), id, isin);
        }
        return id;
    }

    private UUID insertarCuenta(
        UUID portafolioId,
        String nombre,
        UUID tipoCuentaId,
        boolean activa
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into cuenta_financiera
                (id, portafolio_id, institucion_id, tipo_cuenta_id, nombre,
                 moneda_id, activo)
            values (?, ?, ?, ?, ?, ?, ?)
            """, id, portafolioId, institucionId, tipoCuentaId, nombre,
            monedaId, activa);
        return id;
    }

    private UUID insertarPosicion(
        UUID cuentaId,
        UUID instrumentoId,
        String cantidad,
        String costoUnitario,
        boolean activa
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into posicion
                (id, cuenta_id, instrumento_id, cantidad, precio_promedio,
                 moneda_id, activo)
            values (?, ?, ?, ?, ?, ?, ?)
            """, id, cuentaId, instrumentoId,
            new BigDecimal(cantidad), new BigDecimal(costoUnitario),
            monedaId, activa);
        return id;
    }

    private UUID crearUsuario(String email, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, ?)
            """, id, "Usuario de inversiones", email, PASSWORD_HASH, activo);
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

    private void crearMembresia(UUID usuario, UUID portafolio, String rol) {
        jdbcTemplate.update("""
            insert into membresia (id, usuario_id, portafolio_id, rol)
            values (?, ?, ?, ?)
            """, UUID.randomUUID(), usuario, portafolio, rol);
    }

    private MockHttpSession iniciarSesion(String email) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"usuario":"%s","password":"%s"}
                    """.formatted(email, USUARIO_PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

        return (MockHttpSession) resultado.getRequest().getSession(false);
    }
}
