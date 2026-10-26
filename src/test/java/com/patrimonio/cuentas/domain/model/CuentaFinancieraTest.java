package com.patrimonio.cuentas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de dominio sin contexto de Spring, sin base de datos y sin Docker.
 */
class CuentaFinancieraTest {

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

    @Test
    void reconstruir_conDatosCompletos_exponeLosValores() {
        CuentaFinanciera cuenta = reconstruir(
            "Cuenta corriente",
            new NumeroCuenta("ES91 2100 0418 4502 0005 1332"),
            true,
            LocalDate.of(2024, 1, 15),
            null);

        assertThat(cuenta.id()).isEqualTo(ID);
        assertThat(cuenta.portafolioId()).isEqualTo(PORTAFOLIO_ID);
        assertThat(cuenta.institucionId()).isEqualTo(INSTITUCION_ID);
        assertThat(cuenta.tipoCuentaId()).isEqualTo(TIPO_CUENTA_ID);
        assertThat(cuenta.monedaId()).isEqualTo(MONEDA_ID);
        assertThat(cuenta.nombre()).isEqualTo("Cuenta corriente");
        assertThat(cuenta.numeroCuenta()).contains(new NumeroCuenta("ES91 2100 0418 4502 0005 1332"));
        assertThat(cuenta.activo()).isTrue();
        assertThat(cuenta.fechaApertura()).contains(LocalDate.of(2024, 1, 15));
        assertThat(cuenta.fechaCierre()).isEmpty();
    }

    @Test
    void reconstruir_sinNumeroCuentaNiFechas_exponeOpcionalesVacios() {
        CuentaFinanciera cuenta = reconstruir("Cuenta de ahorro", null, false, null, null);

        assertThat(cuenta.numeroCuenta()).isEmpty();
        assertThat(cuenta.fechaApertura()).isEmpty();
        assertThat(cuenta.fechaCierre()).isEmpty();
        assertThat(cuenta.activo()).isFalse();
    }

    @Test
    void reconstruir_conFechaCierreIgualAFechaApertura_esValido() {
        LocalDate fecha = LocalDate.of(2024, 3, 1);

        CuentaFinanciera cuenta = reconstruir("Cuenta cerrada", null, false, fecha, fecha);

        assertThat(cuenta.fechaCierre()).contains(fecha);
    }

    @Test
    void reconstruir_conNombreEnBlanco_falla() {
        assertThatThrownBy(() -> reconstruir("   ", null, true, null, null))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("nombre");
    }

    @Test
    void reconstruir_conNombreDemasiadoLargo_falla() {
        assertThatThrownBy(() -> reconstruir("n".repeat(151), null, true, null, null))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("150");
    }

    @Test
    void reconstruir_conFechaCierreAnteriorAFechaApertura_falla() {
        assertThatThrownBy(() -> reconstruir(
            "Cuenta cerrada",
            null,
            false,
            LocalDate.of(2024, 3, 1),
            LocalDate.of(2024, 2, 29)))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("fecha de cierre");
    }

    @Test
    void reconstruir_sinIdentificador_falla() {
        assertThatThrownBy(() -> CuentaFinanciera.reconstruir(
            null,
            PORTAFOLIO_ID,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            "Cuenta",
            null,
            MONEDA_ID,
            true,
            null,
            null))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("id");
    }

    @Test
    void reconstruir_sinPortafolio_falla() {
        assertThatThrownBy(() -> CuentaFinanciera.reconstruir(
            ID,
            null,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            "Cuenta",
            null,
            MONEDA_ID,
            true,
            null,
            null))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("portafolioId");
    }

    @Test
    void igualdad_seBasaEnElIdentificador() {
        CuentaFinanciera una = reconstruir("Cuenta", null, true, null, null);
        CuentaFinanciera otra = reconstruir("Otro nombre", null, false, null, null);

        assertThat(una).isEqualTo(otra).hasSameHashCodeAs(otra);
    }

    private static CuentaFinanciera reconstruir(
        String nombre,
        NumeroCuenta numeroCuenta,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        return CuentaFinanciera.reconstruir(
            ID,
            PORTAFOLIO_ID,
            INSTITUCION_ID,
            TIPO_CUENTA_ID,
            nombre,
            numeroCuenta,
            MONEDA_ID,
            activo,
            fechaApertura,
            fechaCierre);
    }
}
