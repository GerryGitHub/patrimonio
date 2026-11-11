package com.patrimonio.inversiones.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.patrimonio.testsupport.CsrfTestSupport;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de extremo a extremo del motor de movimientos.
 *
 * <p>Cubren alta, historial, filtros, idempotencia, reversión, aislamiento,
 * reconstrucción y la restricción de edición cuando la posición tiene
 * historial. Usan un PostgreSQL 16 efímero con el esquema real.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class MovimientosApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA_POSICIONES =
        "/api/v1/portafolios/{portafolioId}/posiciones";
    private static final String RUTA_MOVIMIENTOS =
        RUTA_POSICIONES + "/{posicionId}/movimientos";
    private static final String RUTA_MOVIMIENTO =
        "/api/v1/portafolios/{portafolioId}/movimientos/{movimientoId}";
    private static final String RUTA_REVERSION = RUTA_MOVIMIENTO + "/reversion";
    private static final String RUTA_RESUMEN =
        RUTA_POSICIONES + "/{posicionId}/resumen";
    private static final String RUTA_RECONSTRUCCION =
        RUTA_POSICIONES + "/{posicionId}/reconstruccion";

    private static final String USUARIO_EMAIL = "movimientos@example.com";
    private static final String USUARIO_PASSWORD = "Demo1234!";
    private static final String PASSWORD_HASH =
        new BCryptPasswordEncoder().encode(USUARIO_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID monedaId;
    private UUID institucionId;
    private UUID usuarioId;
    private UUID portafolioId;
    private UUID cuentaId;
    private UUID instrumentoId;

    @BeforeEach
    void prepararDatos() {
        monedaId = UUID.randomUUID();
        institucionId = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into moneda (id, codigo, nombre, decimales) values (?, ?, ?, ?)",
            monedaId, "USD", "Dólar estadounidense", 2);
        jdbcTemplate.update(
            "insert into institucion (id, nombre) values (?, ?)",
            institucionId, "Casa de bolsa de prueba");

        UUID tipoCorretaje = insertarTipoCuenta("Cuenta de corretaje");
        UUID claseRentaVariable = insertarClaseActivo("Renta variable");
        UUID tipoAccion = insertarTipoInstrumento(
            claseRentaVariable, "Acción");

        usuarioId = crearUsuario(USUARIO_EMAIL, true);
        portafolioId = crearPortafolio("Portafolio de movimientos", true);
        crearMembresia(usuarioId, portafolioId, "PROPIETARIO");
        cuentaId = insertarCuenta(
            portafolioId, "Cuenta de corretaje", tipoCorretaje, true);
        instrumentoId = insertarInstrumento(
            "Apple", "AAPL", tipoAccion, null, true);
    }

    @AfterEach
    void limpiarDatos() {
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
    void compraYVenta_actualizaSaldosEHistorial() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].tipo").value("APERTURA"))
            .andExpect(jsonPath("$.items[0].cantidadResultante").value("10.0000000000"));

        MvcResult compra = mockMvc.perform(
                post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                    .session(sesion)
                    .with(CsrfTestSupport.conCsrf(mockMvc))
                    .header("Idempotency-Key", "compra-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "tipo": "COMPRA",
                          "cantidad": "5",
                          "precioUnitario": "20",
                          "comisiones": "1",
                          "impuestos": "0.5"
                        }
                        """))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.tipo").value("COMPRA"))
            .andExpect(jsonPath("$.importeBruto").value("100.0000000000"))
            .andExpect(jsonPath("$.importeNeto").value("101.5000000000"))
            .andExpect(jsonPath("$.cantidadResultante").value("15.0000000000"))
            .andExpect(jsonPath("$.costoResultante").value("301.5000000000"))
            .andReturn();
        String compraId = JsonPath.read(
            compra.getResponse().getContentAsString(), "$.id");

        // Reintento con la misma clave: no duplica y devuelve el mismo id.
        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .header("Idempotency-Key", "compra-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "COMPRA",
                      "cantidad": "5",
                      "precioUnitario": "20",
                      "comisiones": "1",
                      "impuestos": "0.5"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(compraId));

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
            .andExpect(jsonPath("$.tipo").value("VENTA"))
            .andExpect(jsonPath("$.importeNeto").value("117.0000000000"))
            .andExpect(jsonPath("$.costoAplicado").value("80.4000000000"))
            .andExpect(jsonPath("$.resultadoRealizado").value("36.6000000000"))
            .andExpect(jsonPath("$.cantidadResultante").value("11.0000000000"))
            .andExpect(jsonPath("$.costoResultante").value("221.1000000000"));

        mockMvc.perform(get(RUTA_POSICIONES + "/{posicionId}",
                portafolioId, posicion).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("11.0000000000"))
            .andExpect(jsonPath("$.costoTotal").value("221.1000000000"))
            .andExpect(jsonPath("$.resultadoRealizado").value("36.6000000000"));

        mockMvc.perform(get(RUTA_RESUMEN, portafolioId, posicion)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("11.0000000000"))
            .andExpect(jsonPath("$.costoTotal").value("221.1000000000"))
            .andExpect(jsonPath("$.resultadoRealizado").value("36.6000000000"))
            .andExpect(jsonPath("$.movimientosConfirmados").value(3));

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.items", hasSize(3)));

        mockMvc.perform(get(RUTA_POSICIONES + "/resumen", portafolioId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resultadoPorMoneda[0].resultado")
                .value("36.6000000000"));
    }

    @Test
    void ventaSuperiorALaDisponible_devuelve422() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"VENTA","cantidad":"11","precioUnitario":"30"}
                    """))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("MOVIMIENTO_INVALIDO"));
    }

    @Test
    void ajusteNegativoQueVaciaLaPosicion_retiraTodoElCosto() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "AJUSTE_NEGATIVO",
                      "cantidad": "10",
                      "precioUnitario": "0",
                      "costoTotalAjuste": "200",
                      "observaciones": "Baja total"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cantidadResultante").value("0.0000000000"))
            .andExpect(jsonPath("$.costoResultante").value("0.0000000000"));
    }

    @Test
    void reversion_restauraSaldosYNoSeRepite() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        MvcResult compra = mockMvc.perform(
                post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                    .session(sesion)
                    .with(CsrfTestSupport.conCsrf(mockMvc))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"tipo":"COMPRA","cantidad":"5","precioUnitario":"20"}
                        """))
            .andExpect(status().isCreated())
            .andReturn();
        String compraId = JsonPath.read(
            compra.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post(RUTA_REVERSION, portafolioId, compraId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"observaciones":"Compra duplicada"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("REVERSION"))
            .andExpect(jsonPath("$.movimientoOrigenId").value(compraId))
            .andExpect(jsonPath("$.cantidadResultante").value("10.0000000000"))
            .andExpect(jsonPath("$.costoResultante").value("200.0000000000"));

        mockMvc.perform(get(RUTA_MOVIMIENTO, portafolioId, compraId)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revertido").value(true));

        mockMvc.perform(post(RUTA_REVERSION, portafolioId, compraId)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("MOVIMIENTO_YA_REVERTIDO"));

        mockMvc.perform(get(RUTA_POSICIONES + "/{posicionId}",
                portafolioId, posicion).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("10.0000000000"))
            .andExpect(jsonPath("$.costoTotal").value("200.0000000000"));
    }

    @Test
    void posicionInactiva_rechazaMovimientos() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(patch(RUTA_POSICIONES + "/{posicionId}/estado",
                portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"activo":false}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"COMPRA","cantidad":"1","precioUnitario":"20"}
                    """))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("MOVIMIENTO_INVALIDO"));
    }

    @Test
    void idempotenciaConDatosDistintos_devuelve409() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .header("Idempotency-Key", "clave-unica")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"COMPRA","cantidad":"1","precioUnitario":"20"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .header("Idempotency-Key", "clave-unica")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"COMPRA","cantidad":"2","precioUnitario":"20"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCIA_CONFLICTO"));
    }

    @Test
    void aislamiento_otroPortafolioDevuelve404() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");
        UUID portafolioAjeno = crearPortafolio("Portafolio ajeno", true);

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioAjeno, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"COMPRA","cantidad":"1","precioUnitario":"20"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioAjeno, posicion)
                .session(sesion))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"));
    }

    @Test
    void historial_filtraPorTipoYFechaYOrdena() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20", "2026-01-01");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "COMPRA",
                      "cantidad": "1",
                      "precioUnitario": "20",
                      "fechaOperacion": "2026-01-05"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tipo": "VENTA",
                      "cantidad": "1",
                      "precioUnitario": "25",
                      "fechaOperacion": "2026-01-10"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .param("tipo", "COMPRA")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].tipo").value("COMPRA"));

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .param("desde", "2026-01-06")
                .param("hasta", "2026-01-31")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].tipo").value("VENTA"));

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .param("orden", "asc")
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].tipo").value("APERTURA"));
    }

    @Test
    void reconstruccion_detectaYReparaElEstadoMaterializado() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"COMPRA","cantidad":"5","precioUnitario":"20"}
                    """))
            .andExpect(status().isCreated());

        jdbcTemplate.update("""
            update posicion
            set cantidad = ?, costo_total = ?, precio_promedio = ?
            where id = ?
            """, new BigDecimal("99"), new BigDecimal("999"),
            new BigDecimal("10"), posicion);

        mockMvc.perform(post(RUTA_RECONSTRUCCION, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.coincide").value(false))
            .andExpect(jsonPath("$.reparada").value(false))
            .andExpect(jsonPath("$.cantidadCalculada").value("15.0000000000"))
            .andExpect(jsonPath("$.costoCalculado").value("300.0000000000"))
            .andExpect(jsonPath("$.cantidadMaterializada").value("99.0000000000"));

        mockMvc.perform(post(RUTA_RECONSTRUCCION, portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reparar":true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reparada").value(true));

        mockMvc.perform(get(RUTA_POSICIONES + "/{posicionId}",
                portafolioId, posicion).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("15.0000000000"))
            .andExpect(jsonPath("$.costoTotal").value("300.0000000000"));

        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void ventasConcurrentes_noSobregiranLaCantidadDisponible() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");
        CsrfTestSupport.TokenCsrf token = CsrfTestSupport.obtener(mockMvc);
        RequestPostProcessor csrf = request -> {
            CsrfTestSupport.adjuntar(request, token);
            return request;
        };

        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Integer> venta = () -> {
            salida.await();
            return mockMvc.perform(
                    post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                        .session(sesion)
                        .with(csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"tipo":"VENTA","cantidad":"6","precioUnitario":"25"}
                            """))
                .andReturn()
                .getResponse()
                .getStatus();
        };

        Future<Integer> primera = ejecutor.submit(venta);
        Future<Integer> segunda = ejecutor.submit(venta);
        salida.countDown();
        List<Integer> estados = List.of(
            primera.get(60, TimeUnit.SECONDS),
            segunda.get(60, TimeUnit.SECONDS));
        ejecutor.shutdownNow();

        // El bloqueo pesimista serializa las ventas: solo una puede consumir
        // las 6 unidades y la otra descubre que ya no hay cantidad suficiente.
        assertThat(estados).containsExactlyInAnyOrder(201, 422);

        mockMvc.perform(get(RUTA_POSICIONES + "/{posicionId}",
                portafolioId, posicion).session(sesion))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("4.0000000000"));
    }

    @Test
    void migracion_creaAperturaYEsIdempotente() throws Exception {
        UUID posicion = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into posicion
                (id, cuenta_id, instrumento_id, cantidad, precio_promedio,
                 costo_total, moneda_id, fecha_apertura)
            values (?, ?, ?, ?, ?, 0, ?, now())
            """, posicion, cuentaId, instrumentoId,
            new BigDecimal("3"), new BigDecimal("10"), monedaId);

        String script = Files.readString(
            Path.of("db", "esquema-entrega3.sql"));

        jdbcTemplate.execute(script);
        assertThat(contarAperturas(posicion)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "select costo_total from posicion where id = ?",
            BigDecimal.class, posicion))
            .isEqualByComparingTo("30");

        // Una segunda ejecución no duplica la apertura ni reescribe el estado.
        jdbcTemplate.execute(script);
        assertThat(contarAperturas(posicion)).isEqualTo(1);
    }

    private int contarAperturas(UUID posicion) {
        Integer total = jdbcTemplate.queryForObject("""
            select count(*)
            from movimiento m
            join tipo_movimiento t on t.id = m.tipo_movimiento_id
            where m.posicion_id = ? and t.nombre = 'APERTURA'
            """, Integer.class, posicion);
        return total == null ? 0 : total;
    }

    @Test
    void posicionConHistorial_rechazaEdicionDeCantidad() throws Exception {
        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(put(RUTA_POSICIONES + "/{posicionId}",
                portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"cantidad":"12","costoUnitario":"20"}
                    """))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.code").value("POSICION_CON_HISTORIAL"));

        mockMvc.perform(put(RUTA_POSICIONES + "/{posicionId}",
                portafolioId, posicion)
                .session(sesion)
                .with(CsrfTestSupport.conCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "cantidad":"10",
                      "costoUnitario":"20",
                      "observaciones":"Solo cambian las observaciones"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidad").value("10.0000000000"))
            .andExpect(jsonPath("$.costoTotal").value("200.0000000000"));
    }

    @Test
    void sinSesionYsinCsrf_seRechaza() throws Exception {
        mockMvc.perform(get(RUTA_MOVIMIENTOS, portafolioId, UUID.randomUUID()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("NO_AUTENTICADO"));

        MockHttpSession sesion = iniciarSesion();
        UUID posicion = crearPosicion(sesion, "10", "20");

        mockMvc.perform(post(RUTA_MOVIMIENTOS, portafolioId, posicion)
                .session(sesion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"tipo":"COMPRA","cantidad":"1","precioUnitario":"20"}
                    """))
            .andExpect(status().isForbidden());
    }

    private UUID crearPosicion(
        MockHttpSession sesion,
        String cantidad,
        String costoUnitario
    ) throws Exception {
        return crearPosicion(sesion, cantidad, costoUnitario, null);
    }

    private UUID crearPosicion(
        MockHttpSession sesion,
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
                """.formatted(cuentaId, instrumentoId, cantidad, costoUnitario)
            : """
                {
                  "cuentaId": "%s",
                  "instrumentoId": "%s",
                  "cantidad": "%s",
                  "costoUnitario": "%s",
                  "fechaApertura": "%s"
                }
                """.formatted(
                cuentaId, instrumentoId, cantidad, costoUnitario, fechaApertura);
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
        UUID portafolio,
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
            """, id, portafolio, institucionId, tipoCuentaId, nombre,
            monedaId, activa);
        return id;
    }

    private UUID crearUsuario(String email, boolean activo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            insert into usuario (id, nombre, email, password_hash, activo)
            values (?, ?, ?, ?, ?)
            """, id, "Usuario de movimientos", email, PASSWORD_HASH, activo);
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
