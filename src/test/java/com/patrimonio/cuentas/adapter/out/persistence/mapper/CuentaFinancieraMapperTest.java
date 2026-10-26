package com.patrimonio.cuentas.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del mapper explícito sin Spring ni base de datos.
 */
class CuentaFinancieraMapperTest {

    private static final UUID ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PORTAFOLIO_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INSTITUCION_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TIPO_CUENTA_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MONEDA_ID =
        UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final CuentaFinancieraMapper mapper = new CuentaFinancieraMapper();

    @Test
    void toDomain_conDatosValidos_mapeaTodosLosCampos() {
        CuentaFinancieraEntity entity = new CuentaFinancieraEntity(
            ID,
            PORTAFOLIO_ID,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            "Cuenta corriente",
            "ES91 2100 0418 4502 0005 1332",
            MONEDA_ID,
            true,
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2025, 6, 30));

        CuentaFinanciera cuenta = mapper.toDomain(entity);

        assertThat(cuenta.id()).isEqualTo(ID);
        assertThat(cuenta.portafolioId()).isEqualTo(PORTAFOLIO_ID);
        assertThat(cuenta.institucionId()).isEqualTo(INSTITUCION_ID);
        assertThat(cuenta.tipoCuentaId()).isEqualTo(TIPO_CUENTA_ID);
        assertThat(cuenta.monedaId()).isEqualTo(MONEDA_ID);
        assertThat(cuenta.nombre()).isEqualTo("Cuenta corriente");
        assertThat(cuenta.numeroCuenta()).isPresent();
        assertThat(cuenta.numeroCuenta().orElseThrow().valor())
            .isEqualTo("ES91 2100 0418 4502 0005 1332");
        assertThat(cuenta.activo()).isTrue();
        assertThat(cuenta.fechaApertura()).contains(LocalDate.of(2024, 1, 15));
        assertThat(cuenta.fechaCierre()).contains(LocalDate.of(2025, 6, 30));
    }

    @Test
    void toDomain_conColumnasOpcionalesNulas_mapeaOpcionalesVacios() {
        CuentaFinancieraEntity entity = entity("Cuenta de ahorro", null, null, null);

        CuentaFinanciera cuenta = mapper.toDomain(entity);

        assertThat(cuenta.numeroCuenta()).isEmpty();
        assertThat(cuenta.fechaApertura()).isEmpty();
        assertThat(cuenta.fechaCierre()).isEmpty();
    }

    @Test
    void toDomain_conNombreEnBlanco_fallaConDatosInconsistentes() {
        CuentaFinancieraEntity entity = entity("   ", null, null, null);

        assertThatThrownBy(() -> mapper.toDomain(entity))
            .isInstanceOf(DatosPersistidosInvalidos.class)
            .hasMessageContaining("invariantes")
            .hasCauseInstanceOf(CuentaInvalidaException.class);
    }

    @Test
    void toDomain_conFechasIncoherentes_fallaConDatosInconsistentes() {
        CuentaFinancieraEntity entity = entity(
            "Cuenta cerrada",
            null,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 2, 29));

        assertThatThrownBy(() -> mapper.toDomain(entity))
            .isInstanceOf(DatosPersistidosInvalidos.class)
            .hasCauseInstanceOf(CuentaInvalidaException.class);
    }

    @Test
    void toDomain_conNumeroCuentaEnBlanco_fallaConDatosInconsistentes() {
        CuentaFinancieraEntity entity = entity("Cuenta", "   ", null, null);

        assertThatThrownBy(() -> mapper.toDomain(entity))
            .isInstanceOf(DatosPersistidosInvalidos.class)
            .hasCauseInstanceOf(CuentaInvalidaException.class);
    }

    private static CuentaFinancieraEntity entity(
        String nombre,
        String numeroCuenta,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        return new CuentaFinancieraEntity(
            ID,
            PORTAFOLIO_ID,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            nombre,
            numeroCuenta,
            MONEDA_ID,
            true,
            fechaApertura,
            fechaCierre);
    }
}
