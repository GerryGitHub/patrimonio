package com.patrimonio.cuentas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;
import org.junit.jupiter.api.Test;

class NumeroCuentaTest {

    @Test
    void crear_conValorValido_conservaElValor() {
        NumeroCuenta numero = new NumeroCuenta("ES91 2100 0418 4502 0005 1332");

        assertThat(numero.valor()).isEqualTo("ES91 2100 0418 4502 0005 1332");
    }

    @Test
    void crear_conValorNulo_falla() {
        assertThatThrownBy(() -> new NumeroCuenta(null))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("número de cuenta");
    }

    @Test
    void crear_conValorEnBlanco_falla() {
        assertThatThrownBy(() -> new NumeroCuenta("   "))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("número de cuenta");
    }

    @Test
    void crear_conLongitudMaxima_esValido() {
        NumeroCuenta numero = new NumeroCuenta("1".repeat(64));

        assertThat(numero.valor()).hasSize(64);
    }

    @Test
    void crear_conMasDe64Caracteres_falla() {
        assertThatThrownBy(() -> new NumeroCuenta("1".repeat(65)))
            .isInstanceOf(CuentaInvalidaException.class)
            .hasMessageContaining("64");
    }
}
