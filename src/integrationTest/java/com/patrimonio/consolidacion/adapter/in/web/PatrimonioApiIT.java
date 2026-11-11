package com.patrimonio.consolidacion.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
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
 * Pruebas de extremo a extremo del Centro de Patrimonio.
 *
 * <p>Usan un PostgreSQL 16 efímero con el esquema real. Cubren consolidación
 * multi-moneda sin conversión, autorización por membresía, resultado realizado
 * sin doble conteo de reversiones, snapshots idempotentes con campos nulos y
 * series históricas construidas solo con datos registrados.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class PatrimonioApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA_POSICIONES =
        "/api/v1/portafolios/{portafolioId}/posiciones";
    private static final String RUTA_MOVIMIENTOS =
        RUTA_POSICIONES + "/{posicionId}/movimientos";
    private static final String RUTA_PATRIMONIO =
        "/api/v1/portafolios/{portafolioId}/patrimonio";
    private static final String RUTA_GLOBAL = "/api/v1/patrimonio";

    private static final String USUARIO_EMAIL = "patrimonio@example.com";
    private static final String USUARIO_PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(USUARIO_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID usdId;
    private UUID eurId;
    private UUID institucionId;
    private UUID usuarioId;
    private UUID portafolioId;
    private UUID cuentaUsdId;
    private UUID cuentaEurId;
    private UUID instrumentoUsdId;
    private UUID instrumentoEurId;

    @BeforeEach
    void prepararDatos() {
        usdId = UUID.randomUUID();
        eurId = UUID.randomUUID();
        institucionId = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into moneda (id, codigo, nombre, decimales) values (?, ?, ?, ?)",
            usdId, "USD", "Dólar estadounidense", 2);
        jdbcTemplate.update(
            "insert into moneda (id, codigo, nombre, decimales) values (?, ?, ?, ?)",
            eurId, "EUR", "Euro", 2);
        jdbcTemplate.update(
            "insert into institucion (id, nombre) values (?, ?)",
            institucionId, "Casa de bolsa de prueba");

        UUID tipoCorretaje = insertarTipoCuenta("Cuenta de corretaje");
        UUID claseRentaVariable = insertarClaseActivo("Renta variable");
        UUID tipoAccion = insertarTipoInstrumento(
            claseRentaVariable, "Acción");

        usuarioId = crearUsuario(USUARIO_EMAIL);
        portafolioId = crearPortafolio("Portafolio consolidado", true);
        crearMembresia(usuarioId, portafolioId, "PROPIETARIO");
        cuentaUsdId = insertarCuenta(
            portafolioId, "Cuenta USD", tipoCorretaje, usdId);
        cuentaEurId = insertarCuenta(
            portafolioId, "Cuenta EUR", tipoCorretaje, eurId);
        instrumentoUsdId = insertarInstrumento(
            "Apple", "AAPL", tipoAccion, usdId);
        instrumentoEurId = insertarInstrumento(
            "Bono europeo", "BOND", tipoAccion, eurId);
    }

    @AfterEach
    void limpiarDatos() {
        jdbcTemplate.update("delete from snapshot_patrimonio");
        jdbcTemplate.update("delete from movimiento");
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
    void elCentroGlobalConsolidaSoloPortafoliosAutorizadosSinSumarMonedas()
        throws Exception {
        MockHttpSession sesion = iniciarSesion();
        crearPosicion(sesion, cuentaUsdId, instrumentoUsdId, "10", "20", null);
        crearPosicion(sesion, cuentaEurId, instrumentoEurId, "5", "20", null);
        UUID ajeno = crearPortafolio("Portafolio ajeno", true);

        mockMvc.perform(get(RUTA_GLOBAL + "/centro").session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.meta.portafolios", hasSize(1)))
            .andExpect(jsonPath("$.meta.portafolios[0].id").value(
                portafolioId.toString()))
            .andExpect(jsonPath("$.meta.estado").value("PARCIAL"))
            .andExpect(jsonPath("$.meta.cobertura.incluye[0]")
                .value("INVERSIONES_A_COSTO"))
            .andExpect(jsonPath("$.resumen.activosRegistrados.estado")
                .value("PARCIAL"))
            .andExpect(jsonPath("$.resumen.activosRegistrados.importes",
                hasSize(2)))
            .andExpect(jsonPath("$.resumen.activosRegistrados.importes[0].codigo")
                .value("EUR"))
            .andExpect(jsonPath("$.resumen.activosRegistrados.importes[0].importe")
                .value("100.0000000000"))
            .andExpect(jsonPath("$.resumen.activosRegistrados.importes[1].codigo")
                .value("USD"))
            .andExpect(jsonPath("$.resumen.activosRegistrados.importes[1].importe")
                .value("200.0000000000"))
            .andExpect(jsonPath("$.resumen.patrimonioNeto.estado")
                .value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.resumen.capitalAportado.estado")
                .value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.resumen.liquidezDisponible.estado")
                .value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.liquidez.efectivoPorCuenta.estado")
                .value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.eventos.estado").value("NO_DISPONIBLE"));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/centro", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumen.activosRegistrados.importes",
                hasSize(2)));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/centro", ajeno)
                .session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    void elResultadoRealizadoIncluyePosicionesCerradasYDescuentaReversiones()
        throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(
            sesion, cuentaUsdId, instrumentoUsdId, "10", "20", null);

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "VENTA",
                      "cantidad": "4",
                      "precioUnitario": "30",
                      "comisiones": "2",
                      "impuestos": "1"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultadoRealizado")
                .value("37.0000000000"));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/resumen", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultadoRealizado.estado")
                .value("DISPONIBLE"))
            .andExpect(jsonPath("$.resultadoRealizado.importes[0].importe")
                .value("37.0000000000"));

        // Venta total de otra posición: queda cerrada (cantidad cero) pero su
        // resultado realizado sigue formando parte del acumulado.
        UUID cerrada = crearPosicion(
            sesion, cuentaUsdId, instrumentoUsdId, "5", "10", null);
        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, cerrada)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"VENTA","cantidad":"5","precioUnitario":"12"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resultadoRealizado")
                .value("10.0000000000"));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/resumen", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultadoRealizado.importes[0].importe")
                .value("47.0000000000"));

        // Revertir la primera venta elimina su efecto: no se cuenta dos veces.
        String ventaId = obtenerUltimoMovimiento(posicion, "VENTA");
        mockMvc.perform(post(
                "/api/v1/portafolios/{portafolioId}/movimientos/{movimientoId}/reversion",
                portafolioId, ventaId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get(RUTA_PATRIMONIO + "/resumen", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultadoRealizado.importes[0].importe")
                .value("10.0000000000"));
    }

    @Test
    void elSnapshotEsIdempotenteYNoRellenaCamposSinDatos() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        crearPosicion(sesion, cuentaUsdId, instrumentoUsdId, "10", "20", null);

        MvcResult primero = mockMvc.perform(
                post(RUTA_PATRIMONIO + "/snapshots", portafolioId)
                    .session(sesion)
                    .with(CsrfTestSupport.conCsrf(mockMvc)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].capitalInvertido")
                .value("200.0000000000"))
            .andExpect(jsonPath("$[0].resultadoRealizado")
                .value("0.0000000000"))
            .andExpect(jsonPath("$[0].efectivo").value(nullValue()))
            .andExpect(jsonPath("$[0].pasivos").value(nullValue()))
            .andExpect(jsonPath("$[0].valorMercado").value(nullValue()))
            .andExpect(jsonPath("$[0].patrimonioNeto").value(nullValue()))
            .andExpect(jsonPath("$[0].cobertura")
                .value("INVERSIONES_A_COSTO"))
            .andExpect(jsonPath("$[0].fuente").value("MANUAL"))
            .andReturn();
        String snapshotId = JsonPath.read(
            primero.getResponse().getContentAsString(), "$[0].id");

        mockMvc.perform(post(RUTA_PATRIMONIO + "/snapshots", portafolioId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[0].id").value(snapshotId));

        Integer total = jdbcTemplate.queryForObject(
            "select count(*) from snapshot_patrimonio where portafolio_id = ?",
            Integer.class, portafolioId);
        assertThat(total).isEqualTo(1);

        mockMvc.perform(get(RUTA_PATRIMONIO + "/snapshots", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(snapshotId));

        mockMvc.perform(post(RUTA_PATRIMONIO + "/snapshots", portafolioId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fecha":"2020-01-01"}
                    """))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("SNAPSHOT_INVALIDO"));
    }

    @Test
    void laEvolucionYLosRendimientosUsanSoloDatosRegistrados()
        throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(
            sesion, cuentaUsdId, instrumentoUsdId, "10", "20", "2026-01-01");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "VENTA",
                      "cantidad": "4",
                      "precioUnitario": "30",
                      "comisiones": "2",
                      "impuestos": "1",
                      "fechaOperacion": "2026-02-10"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get(RUTA_PATRIMONIO + "/evolucion", portafolioId)
                .param("periodo", "TODO")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series[0].codigo")
                .value("COSTO_ADQUISICION"))
            .andExpect(jsonPath("$.series[0].estado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.series[0].porMoneda[0].puntos",
                org.hamcrest.Matchers.not(hasSize(0))))
            .andExpect(jsonPath("$.series[2].codigo").value("VALOR_MERCADO"))
            .andExpect(jsonPath("$.series[2].estado").value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.series[3].codigo").value("PATRIMONIO_NETO"))
            .andExpect(jsonPath("$.series[3].estado").value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.desglosePorClase[0].porMoneda[0].puntos",
                org.hamcrest.Matchers.not(hasSize(0))));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/distribucion", portafolioId)
                .param("dimension", "CLASE_ACTIVO")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dimension").value("CLASE_ACTIVO"))
            .andExpect(jsonPath("$.porMoneda[0].moneda.codigo").value("USD"))
            .andExpect(jsonPath("$.porMoneda[0].total")
                .value("120.0000000000"))
            .andExpect(jsonPath("$.porMoneda[0].segmentos[0].porcentaje")
                .value("100.0000"));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/rendimientos", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultadoRealizado.importes[0].importe")
                .value("37.0000000000"))
            .andExpect(jsonPath("$.comisionesPagadas.importes[0].importe")
                .value("2.0000000000"))
            .andExpect(jsonPath("$.impuestosPagados.importes[0].importe")
                .value("1.0000000000"))
            .andExpect(jsonPath("$.rendimientoPorcentual.estado")
                .value("NO_DISPONIBLE"));

        mockMvc.perform(get(RUTA_PATRIMONIO + "/liquidez", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.efectivoPorCuenta.estado")
                .value("NO_DISPONIBLE"))
            .andExpect(jsonPath("$.capitalComprometido.importes[0].importe")
                .value("120.0000000000"));
    }

    private String obtenerUltimoMovimiento(UUID posicion, String tipo) {
        return jdbcTemplate.queryForObject("""
            select m.id::text
            from movimiento m
            join tipo_movimiento t on t.id = m.tipo_movimiento_id
            where m.posicion_id = ? and t.nombre = ?
            order by m.fecha_creacion desc
            limit 1
            """, String.class, posicion, tipo);
    }

    private UUID crearPosicion(
        MockHttpSession sesion,
        UUID cuenta,
        UUID instrumento,
        String cantidad,
        String costoUnitario,
        String fechaApertura
    ) throws Exception {
        String cuerpo = fechaApertura == null
            ? """
                {
                  "cuentaId": "%s",
                  "instrumentoId": "%s",
                  "cantidad": "%s",
                  "costoUnitario": "%s"
                }
                """.formatted(cuenta, instrumento, cantidad, costoUnitario)
            : """
                {
                  "cuentaId": "%s",
                  "instrumentoId": "%s",
                  "cantidad": "%s",
                  "costoUnitario": "%s",
                  "fechaApertura": "%s"
                }
                """.formatted(
                cuenta, instrumento, cantidad, costoUnitario, fechaApertura);
        MvcResult resultado = mockMvc.perform(
                post(RUTA_POSICIONES, portafolioId)
                    .session(sesion)
                    .with(CsrfTestSupport.conCsrf(mockMvc))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cuerpo))
            .andExpect(status().isCreated())
            .andReturn();
        String location = resultado.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        return UUID.fromString(
            location.substring(location.lastIndexOf('/') + 1));
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
        UUID monedaId
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into instrumento
                (id, tipo_instrumento_id, nombre, simbolo, moneda_id, activo)
            values (?, ?, ?, ?, ?, true)
            """, id, tipoInstrumentoId, nombre, simbolo, monedaId);
        return id;
    }

    private UUID insertarCuenta(
        UUID portafolio,
        String nombre,
        UUID tipoCuentaId,
        UUID monedaId
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into cuenta_financiera
                (id, portafolio_id, institucion_id, tipo_cuenta_id, nombre,
                 moneda_id, activo)
            values (?, ?, ?, ?, ?, ?, true)
            """, id, portafolio, institucionId, tipoCuentaId, nombre, monedaId);
        return id;
    }

    private UUID crearUsuario(String email) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, true)
            """, id, "Usuario de patrimonio", email, PASSWORD_HASH);
        return id;
    }

    private UUID crearPortafolio(String nombre, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into portafolio (id, nombre, moneda_base_id, activo)
            values (?, ?, ?, ?)
            """, id, nombre, usdId, activo);
        return id;
    }

    private void crearMembresia(UUID usuario, UUID portafolio, String rol) {
        jdbcTemplate.update("""
            insert into membresia (id, usuario_id, portafolio_id, rol)
            values (?, ?, ?, ?)
            """, UUID.randomUUID(), usuario, portafolio, rol);
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
