package com.patrimonio.cuentas.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.acceso.adapter.out.persistence.adapter.PortafolioQueryAdapter;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.cuentas.adapter.out.persistence.adapter.CuentaFinancieraRepositoryAdapter;
import com.patrimonio.cuentas.adapter.out.persistence.mapper.CuentaFinancieraMapper;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.testsupport.PostgresTestContainer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Pruebas del adaptador JPA contra el esquema real en un PostgreSQL 16 efímero.
 *
 * <p>Verifican el mapeo explícito, el filtro por portafolio y la traducción de
 * datos incoherentes. No usan la base de desarrollo.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers
@Import({
    CuentaFinancieraRepositoryAdapter.class,
    CuentaFinancieraMapper.class,
    PortafolioQueryAdapter.class
})
class CuentaFinancieraRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    @Autowired
    private CuentaFinancieraRepositoryAdapter adapter;

    @Autowired
    private PortafolioQueryPort portafolioQueryPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID monedaId;
    private UUID institucionId;
    private UUID tipoCuentaId;

    @BeforeEach
    void setUp() {
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

    @Test
    void listarPorPortafolio_devuelveSoloLasCuentasDelPortafolioSolicitado() {
        UUID portafolioA = crearPortafolio("Portafolio A");
        UUID portafolioB = crearPortafolio("Portafolio B");

        UUID cuentaAhorro = insertarCuenta(
            portafolioA, "Ahorro", "ES91 2100 0418 4502 0005 1332",
            true, LocalDate.of(2024, 1, 15), null);
        UUID cuentaCorriente = insertarCuenta(
            portafolioA, "Corriente", null, false, null, null);
        UUID cuentaAjena = insertarCuenta(
            portafolioB, "Cuenta de otro portafolio", null, true, null, null);

        List<CuentaFinanciera> cuentas = adapter.listarPorPortafolio(portafolioA);

        assertThat(cuentas).extracting(CuentaFinanciera::id)
            .containsExactly(cuentaAhorro, cuentaCorriente)
            .doesNotContain(cuentaAjena);

        CuentaFinanciera ahorro = cuentas.get(0);
        assertThat(ahorro.portafolioId()).isEqualTo(portafolioA);
        assertThat(ahorro.institucionId()).isEqualTo(institucionId);
        assertThat(ahorro.tipoCuentaId()).isEqualTo(tipoCuentaId);
        assertThat(ahorro.monedaId()).isEqualTo(monedaId);
        assertThat(ahorro.nombre()).isEqualTo("Ahorro");
        assertThat(ahorro.numeroCuenta()).isPresent();
        assertThat(ahorro.numeroCuenta().orElseThrow().valor())
            .isEqualTo("ES91 2100 0418 4502 0005 1332");
        assertThat(ahorro.activo()).isTrue();
        assertThat(ahorro.fechaApertura()).contains(LocalDate.of(2024, 1, 15));
        assertThat(ahorro.fechaCierre()).isEmpty();

        CuentaFinanciera corriente = cuentas.get(1);
        assertThat(corriente.numeroCuenta()).isEmpty();
        assertThat(corriente.activo()).isFalse();
        assertThat(corriente.fechaApertura()).isEmpty();
        assertThat(corriente.fechaCierre()).isEmpty();
    }

    @Test
    void listarPorPortafolio_cuandoNoTieneCuentas_devuelveListaVacia() {
        UUID portafolio = crearPortafolio("Portafolio vacío");

        assertThat(adapter.listarPorPortafolio(portafolio)).isEmpty();
    }

    @Test
    void listarPorPortafolio_cuandoElNombreAlmacenadoEsIncoherente_falla() {
        UUID portafolio = crearPortafolio("Portafolio con datos corruptos");
        insertarCuenta(portafolio, "   ", null, true, null, null);

        assertThatThrownBy(() -> adapter.listarPorPortafolio(portafolio))
            .isInstanceOf(DatosPersistidosInvalidos.class);
    }

    @Test
    void buscarPorId_cuandoElPortafolioExiste_devuelveSuResumen() {
        UUID portafolio = crearPortafolio("Portafolio existente");

        Optional<PortafolioResumen> resumen = portafolioQueryPort.buscarPorId(portafolio);

        assertThat(resumen).isPresent();
        assertThat(resumen.orElseThrow().id()).isEqualTo(portafolio);
        assertThat(resumen.orElseThrow().nombre()).isEqualTo("Portafolio existente");
        assertThat(resumen.orElseThrow().monedaBaseId()).isEqualTo(monedaId);
        assertThat(resumen.orElseThrow().activo()).isTrue();
    }

    @Test
    void buscarPorId_cuandoElPortafolioNoExiste_devuelveVacio() {
        assertThat(portafolioQueryPort.buscarPorId(UUID.randomUUID())).isEmpty();
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
