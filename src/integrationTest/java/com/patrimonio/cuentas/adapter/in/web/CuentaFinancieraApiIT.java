package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.adapter.out.persistence.repository.CuentaFinancieraJpaRepository;
import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
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
 * Pruebas de extremo a extremo del endpoint de cuentas con autenticación y
 * autorización por membresía.
 *
 * <p>Arrancan el contexto completo de Spring, la seguridad, el adaptador REST,
 * el caso de uso y la persistencia JPA contra un PostgreSQL 16 efímero de
 * Testcontainers. No se mockea ningún puerto y no se usa la base de
 * desarrollo.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class CuentaFinancieraApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA = "/api/v1/portafolios/{portafolioId}/cuentas";
    private static final String USUARIO_EMAIL = "titular@example.com";
    private static final String USUARIO_PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(USUARIO_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CuentaFinancieraJpaRepository cuentasJpaRepository;

    private UUID monedaId;
    private UUID institucionId;
    private UUID tipoCuentaId;
    private UUID usuarioId;

    @BeforeEach
    void prepararCatalogos() {
        monedaId = UUID.randomUUID();
        institucionId = UUID.randomUUID();
        tipoCuentaId = UUID.randomUUID();

        jdbcTemplate.update(
            "insert into moneda (id, codigo, nombre, decimales) values (?, ?, ?, ?)",
            monedaId, "USD", "Dólar estadounidense", 2);
        jdbcTemplate.update(
            "insert into institucion (id, nombre) values (?, ?)",
            institucionId, "Banco de prueba");
        jdbcTemplate.update(
            "insert into tipo_cuenta (id, nombre) values (?, ?)",
            tipoCuentaId, "Corriente");

        usuarioId = crearUsuario(USUARIO_EMAIL, true);
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        // Orden inverso a las claves foráneas y solo dentro del contenedor.
        jdbcTemplate.update("delete from membresia");
        jdbcTemplate.update("delete from cuenta_financiera");
        jdbcTemplate.update("delete from portafolio");
        jdbcTemplate.update("delete from usuario");
        jdbcTemplate.update("delete from institucion");
        jdbcTemplate.update("delete from tipo_cuenta");
        jdbcTemplate.update("delete from moneda");
    }

    @Test
    void listar_devuelveSoloLasCuentasDelPortafolioYElContratoCompleto() throws Exception {
        UUID portafolioA = crearPortafolio("Portafolio A", true);
        UUID portafolioB = crearPortafolio("Portafolio B", true);
        crearMembresia(usuarioId, portafolioA);
        crearMembresia(usuarioId, portafolioB);
        MockHttpSession sesion = iniciarSesion();

        UUID cuentaAhorro = insertarCuenta(
            portafolioA, "Ahorro", "ES91 2100 0418 4502 0005 1332",
            true, LocalDate.of(2024, 1, 15), null);
        UUID cuentaCorriente = insertarCuenta(
            portafolioA, "Corriente", null, false, null, null);
        UUID cuentaAjena = insertarCuenta(
            portafolioB, "Cuenta ajena", null, true, null, null);

        // Fila PostgreSQL -> entidad JPA.
        CuentaFinancieraEntity entidad = cuentasJpaRepository.findById(cuentaAhorro)
            .orElseThrow();
        assertThat(entidad.getPortafolioId()).isEqualTo(portafolioA);
        assertThat(entidad.getInstitucionId()).isEqualTo(institucionId);
        assertThat(entidad.getTipoCuentaId()).isEqualTo(tipoCuentaId);
        assertThat(entidad.getMonedaId()).isEqualTo(monedaId);
        assertThat(entidad.getNombre()).isEqualTo("Ahorro");
        assertThat(entidad.getNumeroCuenta())
            .isEqualTo("ES91 2100 0418 4502 0005 1332");
        assertThat(entidad.isActivo()).isTrue();
        assertThat(entidad.getFechaApertura()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(entidad.getFechaCierre()).isNull();

        // Entidad/dominio -> DTO de respuesta, filtrado por portafolio.
        mockMvc.perform(get(RUTA, portafolioA).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].id",
                contains(cuentaAhorro.toString(), cuentaCorriente.toString())))
            .andExpect(jsonPath("$[0].nombre").value("Ahorro"))
            .andExpect(jsonPath("$[0].numeroCuenta").value("****1332"))
            .andExpect(jsonPath("$[0].institucionId").value(institucionId.toString()))
            .andExpect(jsonPath("$[0].tipoCuentaId").value(tipoCuentaId.toString()))
            .andExpect(jsonPath("$[0].monedaId").value(monedaId.toString()))
            .andExpect(jsonPath("$[0].activo").value(true))
            .andExpect(jsonPath("$[0].fechaApertura").value("2024-01-15"))
            .andExpect(jsonPath("$[0].fechaCierre").value(nullValue()))
            .andExpect(jsonPath("$[1].id").value(cuentaCorriente.toString()))
            .andExpect(jsonPath("$[1].nombre").value("Corriente"))
            .andExpect(jsonPath("$[1].numeroCuenta").value(nullValue()))
            .andExpect(jsonPath("$[1].activo").value(false))
            .andExpect(jsonPath("$[1].fechaApertura").value(nullValue()))
            .andExpect(jsonPath("$[1].fechaCierre").value(nullValue()))
            .andExpect(jsonPath("$[0].portafolioId").doesNotExist())
            .andExpect(jsonPath("$[0].hibernateLazyInitializer").doesNotExist())
            .andExpect(content().string(not(containsString(cuentaAjena.toString()))))
            .andExpect(content().string(not(containsString("Cuenta ajena"))))
            .andExpect(content().string(not(containsString("ES91 2100"))));

        // El otro portafolio devuelve únicamente su propia cuenta.
        mockMvc.perform(get(RUTA, portafolioB).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(cuentaAjena.toString()))
            .andExpect(content().string(not(containsString(cuentaAhorro.toString()))));
    }

    @Test
    void listar_cuandoElNumeroEsCorto_loEnmascaraCompletamente() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio con número corto", true);
        crearMembresia(usuarioId, portafolio);
        MockHttpSession sesion = iniciarSesion();
        insertarCuenta(portafolio, "Cuenta corta", "123", true, null, null);

        mockMvc.perform(get(RUTA, portafolio).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].numeroCuenta").value("***"));
    }

    @Test
    void listar_cuandoElPortafolioNoTieneCuentas_devuelve200ConArrayVacio()
        throws Exception {
        UUID portafolioVacio = crearPortafolio("Portafolio vacío", true);
        UUID otroPortafolio = crearPortafolio("Portafolio con cuentas", true);
        crearMembresia(usuarioId, portafolioVacio);
        crearMembresia(usuarioId, otroPortafolio);
        MockHttpSession sesion = iniciarSesion();
        insertarCuenta(otroPortafolio, "Cuenta", null, true, null, null);

        mockMvc.perform(get(RUTA, portafolioVacio).session(sesion))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)))
            .andExpect(content().string("[]"));
    }

    @Test
    void listar_cuandoElIdentificadorNoEsUuid_devuelve400ProblemDetail()
        throws Exception {
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get(RUTA, "no-es-un-uuid").session(sesion))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("ID_INVALIDO"))
            .andExpect(jsonPath("$.detail").value(containsString("portafolioId")))
            .andExpect(content().string(not(containsString("MethodArgumentTypeMismatch"))));
    }

    @Test
    void listar_cuandoElPortafolioNoExiste_devuelve404ProblemDetail()
        throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID portafolioInexistente = UUID.randomUUID();

        mockMvc.perform(get(RUTA, portafolioInexistente).session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
            .andExpect(jsonPath("$.detail")
                .value("No existe el portafolio indicado."));
    }

    @Test
    void listar_cuandoElUsuarioNoTieneMembresia_devuelve404SinRevelarElPortafolio()
        throws Exception {
        UUID portafolioAjeno = crearPortafolio("Portafolio ajeno", true);
        insertarCuenta(portafolioAjeno, "Cuenta ajena", null, true, null, null);
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get(RUTA, portafolioAjeno).session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
            .andExpect(jsonPath("$.detail")
                .value("No existe el portafolio indicado."))
            .andExpect(content().string(not(containsString("Cuenta ajena"))));
    }

    @Test
    void listar_cuandoElPortafolioEstaInactivo_devuelve404() throws Exception {
        UUID portafolioInactivo = crearPortafolio("Portafolio inactivo", false);
        crearMembresia(usuarioId, portafolioInactivo);
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(get(RUTA, portafolioInactivo).session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    void listar_sinSesion_devuelve401ProblemDetail() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio A", true);
        crearMembresia(usuarioId, portafolio);

        mockMvc.perform(get(RUTA, portafolio))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"))
            .andExpect(content().string(not(containsString("No existe"))));
    }

    @Test
    void listar_cuandoLosDatosAlmacenadosSonIncoherentes_devuelve500SinDetalleInterno(
        CapturedOutput output
    ) throws Exception {
        UUID portafolio = crearPortafolio("Portafolio con datos corruptos", true);
        crearMembresia(usuarioId, portafolio);
        MockHttpSession sesion = iniciarSesion();
        insertarCuenta(
            portafolio, "   ", "ES91 2100 0418 4502 0005 1332", true, null, null);

        mockMvc.perform(get(RUTA, portafolio).session(sesion))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("DATOS_INCONSISTENTES"))
            .andExpect(jsonPath("$.detail")
                .value("Los datos almacenados no son coherentes."))
            .andExpect(content().string(not(containsString("invariantes"))))
            .andExpect(content().string(not(containsString("CuentaFinanciera"))))
            .andExpect(content().string(not(containsString("ES91 2100"))));

        assertThat(output.getAll())
            .as("el número completo no debe aparecer en los logs")
            .doesNotContain("ES91 2100");
    }

    @Test
    void listar_filtraPorInstitucionTipoYEstado() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio con filtros", true);
        crearMembresia(usuarioId, portafolio);
        MockHttpSession sesion = iniciarSesion();

        UUID otraInstitucion = UUID.randomUUID();
        UUID otroTipo = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into institucion (id, nombre, tipo) values (?, ?, ?)",
            otraInstitucion, "Otra institución", "BANCO");
        jdbcTemplate.update(
            "insert into tipo_cuenta (id, nombre) values (?, ?)",
            otroTipo, "Inversión");

        insertarCuentaEn(
            portafolio, "Cuenta principal", institucionId, tipoCuentaId,
            "1111", true);
        insertarCuentaEn(
            portafolio, "Cuenta de otra institución", otraInstitucion, tipoCuentaId,
            "2222", true);
        insertarCuentaEn(
            portafolio, "Cuenta de otro tipo", institucionId, otroTipo,
            "3333", true);
        insertarCuentaEn(
            portafolio, "Cuenta inactiva", institucionId, tipoCuentaId,
            "4444", false);

        mockMvc.perform(get(RUTA, portafolio)
                .param("institucionId", institucionId.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get(RUTA, portafolio)
                .param("tipoCuentaId", otroTipo.toString())
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Cuenta de otro tipo"));

        mockMvc.perform(get(RUTA, portafolio)
                .param("activo", "false")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].nombre").value("Cuenta inactiva"));
    }

    @Test
    void consultar_devuelveElDetalleEnmascaradoYRechazaCuentasDeOtroPortafolio()
        throws Exception {
        UUID portafolio = crearPortafolio("Portafolio propio", true);
        UUID otroPortafolio = crearPortafolio("Portafolio ajeno", true);
        crearMembresia(usuarioId, portafolio);
        MockHttpSession sesion = iniciarSesion();
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta propia", "ES91 2100 0418 4502 0005 1332",
            true, LocalDate.of(2024, 1, 15), null);
        UUID ajena = insertarCuenta(
            otroPortafolio, "Cuenta ajena", null, true, null, null);

        mockMvc.perform(get(RUTA + "/{cuentaId}", portafolio, cuenta)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(cuenta.toString()))
            .andExpect(jsonPath("$.nombre").value("Cuenta propia"))
            .andExpect(jsonPath("$.numeroCuenta").value("****1332"))
            .andExpect(content().string(not(containsString("ES91 2100"))));

        mockMvc.perform(get(RUTA + "/{cuentaId}", portafolio, ajena)
                .session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
            .andExpect(content().string(not(containsString("Cuenta ajena"))));
    }

    @Test
    void crear_conPermisoDeEditor_guardaLaCuentaYEnmascaraLaRespuesta()
        throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de alta", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        MockHttpSession sesion = iniciarSesion();

        mockMvc.perform(post(RUTA, portafolio)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "institucionId": "%s",
                      "tipoCuentaId": "%s",
                      "monedaId": "%s",
                      "nombre": "  Cuenta   nueva ",
                      "descripcion": "Detalle de la cuenta",
                      "numeroCuenta": "ES91 2100 0418 4502 0005 1332",
                      "fechaApertura": "2024-01-15"
                    }
                    """.formatted(institucionId, tipoCuentaId, monedaId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString(
                "/api/v1/portafolios/" + portafolio + "/cuentas/")))
            .andExpect(jsonPath("$.nombre").value("Cuenta nueva"))
            .andExpect(jsonPath("$.numeroCuenta").value("****1332"))
            .andExpect(jsonPath("$.descripcion").value("Detalle de la cuenta"))
            .andExpect(jsonPath("$.activo").value(true))
            .andExpect(jsonPath("$.fechaApertura").value("2024-01-15"))
            .andExpect(content().string(not(containsString("ES91 2100"))));

        assertThat(jdbcTemplate.queryForObject(
            "select numero_cuenta from cuenta_financiera where portafolio_id = ?",
            String.class,
            portafolio))
            .isEqualTo("ES91 2100 0418 4502 0005 1332");
        assertThat(jdbcTemplate.queryForObject(
            "select descripcion from cuenta_financiera where portafolio_id = ?",
            String.class,
            portafolio))
            .isEqualTo("Detalle de la cuenta");
    }

    @Test
    void crear_conNumeroDuplicadoEnLaInstitucion_devuelve409() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio duplicado", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        MockHttpSession sesion = iniciarSesion();
        insertarCuenta(portafolio, "Cuenta existente", "123456", true, null, null);

        mockMvc.perform(post(RUTA, portafolio)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "institucionId": "%s",
                      "tipoCuentaId": "%s",
                      "monedaId": "%s",
                      "nombre": "Cuenta duplicada",
                      "numeroCuenta": "123456"
                    }
                    """.formatted(institucionId, tipoCuentaId, monedaId)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CUENTA_DUPLICADA"));
    }

    @Test
    void crear_conRolLector_devuelve403() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de solo lectura", true);
        crearMembresia(usuarioId, portafolio);

        mockMvc.perform(post(RUTA, portafolio)
                .session(iniciarSesion())
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "institucionId": "%s",
                      "tipoCuentaId": "%s",
                      "monedaId": "%s",
                      "nombre": "Cuenta"
                    }
                    """.formatted(institucionId, tipoCuentaId, monedaId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void crear_conInstitucionInexistente_devuelve422() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio con relación inválida", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");

        mockMvc.perform(post(RUTA, portafolio)
                .session(iniciarSesion())
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "institucionId": "%s",
                      "tipoCuentaId": "%s",
                      "monedaId": "%s",
                      "nombre": "Cuenta"
                    }
                    """.formatted(UUID.randomUUID(), tipoCuentaId, monedaId)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("CUENTA_INVALIDA"));
    }

    @Test
    void actualizar_modificaLaCuentaYNoAceptaCuentasDeOtroPortafolio()
        throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de edición", true);
        UUID otroPortafolio = crearPortafolio("Portafolio ajeno", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        MockHttpSession sesion = iniciarSesion();
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta original", "1234", true,
            LocalDate.of(2024, 1, 1), null);
        UUID ajena = insertarCuenta(
            otroPortafolio, "Cuenta ajena", null, true, null, null);

        mockMvc.perform(put(RUTA + "/{cuentaId}", portafolio, cuenta)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "institucionId": "%s",
                      "tipoCuentaId": "%s",
                      "monedaId": "%s",
                      "nombre": "Cuenta editada",
                      "descripcion": "Nueva descripción",
                      "numeroCuenta": "987654",
                      "fechaApertura": "2024-02-01"
                    }
                    """.formatted(institucionId, tipoCuentaId, monedaId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombre").value("Cuenta editada"))
            .andExpect(jsonPath("$.descripcion").value("Nueva descripción"))
            .andExpect(jsonPath("$.numeroCuenta").value("****7654"))
            .andExpect(jsonPath("$.fechaApertura").value("2024-02-01"));

        mockMvc.perform(put(RUTA + "/{cuentaId}", portafolio, ajena)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "institucionId": "%s",
                      "tipoCuentaId": "%s",
                      "monedaId": "%s",
                      "nombre": "Intento ajeno"
                    }
                    """.formatted(institucionId, tipoCuentaId, monedaId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void cambiarEstado_desactivaYReactivaLaCuenta() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de estado", true);
        crearMembresia(usuarioId, portafolio, "EDITOR");
        MockHttpSession sesion = iniciarSesion();
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta de estado", null, true,
            LocalDate.of(2024, 1, 1), null);

        mockMvc.perform(patch(RUTA + "/{cuentaId}/estado", portafolio, cuenta)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(false))
            .andExpect(jsonPath("$.fechaCierre").isNotEmpty());

        mockMvc.perform(patch(RUTA + "/{cuentaId}/estado", portafolio, cuenta)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activo").value(true))
            .andExpect(jsonPath("$.fechaCierre").value(nullValue()));
    }

    @Test
    void cambiarEstado_conRolLector_devuelve403() throws Exception {
        UUID portafolio = crearPortafolio("Portafolio de solo lectura", true);
        crearMembresia(usuarioId, portafolio);
        UUID cuenta = insertarCuenta(
            portafolio, "Cuenta", null, true, null, null);

        mockMvc.perform(patch(RUTA + "/{cuentaId}/estado", portafolio, cuenta)
                .session(iniciarSesion())
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"activo\": false}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESO_DENEGADO"));
    }

    @Test
    void tiposCuenta_listaSoloLosActivos() throws Exception {
        jdbcTemplate.update("""
            insert into tipo_cuenta (id, nombre, activo) values (?, ?, false)
            """, UUID.randomUUID(), "Tipo inactivo");

        mockMvc.perform(get("/api/v1/tipos-cuenta").session(iniciarSesion()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(tipoCuentaId.toString()))
            .andExpect(jsonPath("$[0].nombre").value("Corriente"))
            .andExpect(jsonPath("$[0].activo").value(true));
    }

    private UUID insertarCuentaEn(
        UUID portafolioId,
        String nombre,
        UUID institucion,
        UUID tipoCuenta,
        String numeroCuenta,
        boolean activo
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into cuenta_financiera
                (id, portafolio_id, institucion_id, tipo_cuenta_id, nombre,
                 numero_cuenta, moneda_id, activo)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id, portafolioId, institucion, tipoCuenta, nombre,
            numeroCuenta, monedaId, activo);
        return id;
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

    private UUID crearUsuario(String email, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, ?)
            """, id, "Usuario de prueba", email, PASSWORD_HASH, activo);
        return id;
    }

    private void crearMembresia(UUID usuario, UUID portafolio) {
        crearMembresia(usuario, portafolio, "LECTOR");
    }

    private void crearMembresia(UUID usuario, UUID portafolio, String rol) {
        jdbcTemplate.update("""
            insert into membresia (id, usuario_id, portafolio_id, rol)
            values (?, ?, ?, ?)
            """, UUID.randomUUID(), usuario, portafolio, rol);
    }

    private UUID crearPortafolio(String nombre, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into portafolio (id, nombre, moneda_base_id, activo)
            values (?, ?, ?, ?)
            """, id, nombre, monedaId, activo);
        return id;
    }

    private UUID insertarCuenta(
        UUID portafolioId,
        String nombre,
        String numeroCuenta,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into cuenta_financiera
                (id, portafolio_id, institucion_id, tipo_cuenta_id, nombre,
                 numero_cuenta, moneda_id, activo, fecha_apertura, fecha_cierre)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            id, portafolioId, institucionId, tipoCuentaId, nombre,
            numeroCuenta, monedaId, activo, fechaApertura, fechaCierre);
        return id;
    }
}
