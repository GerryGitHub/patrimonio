package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.adapter.out.persistence.repository.CuentaFinancieraJpaRepository;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.time.LocalDate;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas de extremo a extremo del endpoint de cuentas.
 *
 * <p>Arrancan el contexto completo de Spring, el adaptador REST, el caso de uso
 * y la persistencia JPA contra un PostgreSQL 16 efímero de Testcontainers. No
 * se mockea ningún puerto y no se usa la base de desarrollo.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class CuentaFinancieraApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    private static final String RUTA = "/api/v1/portafolios/{portafolioId}/cuentas";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CuentaFinancieraJpaRepository cuentasJpaRepository;

    private UUID monedaId;
    private UUID institucionId;
    private UUID tipoCuentaId;

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
    }

    @AfterEach
    void limpiarDatosDePrueba() {
        // Orden inverso a las claves foráneas y solo dentro del contenedor.
        jdbcTemplate.update("delete from cuenta_financiera");
        jdbcTemplate.update("delete from portafolio");
        jdbcTemplate.update("delete from institucion");
        jdbcTemplate.update("delete from tipo_cuenta");
        jdbcTemplate.update("delete from moneda");
    }

    @Test
    void listar_devuelveSoloLasCuentasDelPortafolioYElContratoCompleto() throws Exception {
        UUID portafolioA = crearPortafolio("Portafolio A");
        UUID portafolioB = crearPortafolio("Portafolio B");

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
        mockMvc.perform(get(RUTA, portafolioA))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[*].id",
                contains(cuentaAhorro.toString(), cuentaCorriente.toString())))
            .andExpect(jsonPath("$[0].nombre").value("Ahorro"))
            .andExpect(jsonPath("$[0].numeroCuenta")
                .value("ES91 2100 0418 4502 0005 1332"))
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
            .andExpect(content().string(not(containsString("Cuenta ajena"))));

        // El otro portafolio devuelve únicamente su propia cuenta.
        mockMvc.perform(get(RUTA, portafolioB))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(cuentaAjena.toString()))
            .andExpect(content().string(not(containsString(cuentaAhorro.toString()))));
    }

    @Test
    void listar_cuandoElPortafolioNoTieneCuentas_devuelve200ConArrayVacio()
        throws Exception {
        UUID portafolioVacio = crearPortafolio("Portafolio vacío");
        UUID otroPortafolio = crearPortafolio("Portafolio con cuentas");
        insertarCuenta(otroPortafolio, "Cuenta", null, true, null, null);

        mockMvc.perform(get(RUTA, portafolioVacio))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(0)))
            .andExpect(content().string("[]"));
    }

    @Test
    void listar_cuandoElIdentificadorNoEsUuid_devuelve400ProblemDetail()
        throws Exception {
        mockMvc.perform(get(RUTA, "no-es-un-uuid"))
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
        UUID portafolioInexistente = UUID.randomUUID();

        mockMvc.perform(get(RUTA, portafolioInexistente))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NO_ENCONTRADO"))
            .andExpect(jsonPath("$.detail")
                .value("No existe el portafolio indicado."));
    }

    @Test
    void listar_cuandoLosDatosAlmacenadosSonIncoherentes_devuelve500SinDetalleInterno()
        throws Exception {
        UUID portafolio = crearPortafolio("Portafolio con datos corruptos");
        insertarCuenta(portafolio, "   ", null, true, null, null);

        mockMvc.perform(get(RUTA, portafolio))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("DATOS_INCONSISTENTES"))
            .andExpect(jsonPath("$.detail")
                .value("Los datos almacenados no son coherentes."))
            .andExpect(content().string(not(containsString("invariantes"))))
            .andExpect(content().string(not(containsString("CuentaFinanciera"))));
    }

    private UUID crearPortafolio(String nombre) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
            "insert into portafolio (id, nombre, moneda_base_id) values (?, ?, ?)",
            id, nombre, monedaId);
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
