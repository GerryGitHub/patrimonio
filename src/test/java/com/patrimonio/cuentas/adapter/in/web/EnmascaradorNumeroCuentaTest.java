package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnmascaradorNumeroCuentaTest {

    @Test
    void enmascaraUnNumeroLargoConservandoSoloLosUltimosCuatroCaracteres() {
        assertThat(EnmascaradorNumeroCuenta.enmascarar("ES91 2100 0418 4502 0005 1332"))
            .isEqualTo("****1332");
    }

    @Test
    void enmascaraPorCompletoUnNumeroDeCuatroCaracteres() {
        assertThat(EnmascaradorNumeroCuenta.enmascarar("1234"))
            .isEqualTo("****");
    }

    @Test
    void enmascaraPorCompletoUnNumeroCorto() {
        assertThat(EnmascaradorNumeroCuenta.enmascarar("12"))
            .isEqualTo("**");
    }

    @Test
    void devuelveNullCuandoElNumeroEsNulo() {
        assertThat(EnmascaradorNumeroCuenta.enmascarar(null)).isNull();
    }

    @Test
    void devuelveNullCuandoElNumeroEstaVacioOEnBlanco() {
        assertThat(EnmascaradorNumeroCuenta.enmascarar("")).isNull();
        assertThat(EnmascaradorNumeroCuenta.enmascarar("   ")).isNull();
    }

    @Test
    void ignoraLosEspaciosExterioresAntesDeEnmascarar() {
        assertThat(EnmascaradorNumeroCuenta.enmascarar("  12345  "))
            .isEqualTo("****2345");
    }

    @Test
    void noExponeElValorOriginalEnLaMascara() {
        String numero = "ES9121000418450200051332";

        String enmascarado = EnmascaradorNumeroCuenta.enmascarar(numero);

        assertThat(enmascarado)
            .isEqualTo("****1332")
            .doesNotContain("ES91")
            .doesNotContain("2100");
    }
}
