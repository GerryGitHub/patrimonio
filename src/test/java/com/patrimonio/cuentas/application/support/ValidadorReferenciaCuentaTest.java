package com.patrimonio.cuentas.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.domain.error.CuentaDuplicadaException;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas de la resolución y validación de la referencia bancaria.
 */
@ExtendWith(MockitoExtension.class)
class ValidadorReferenciaCuentaTest {

    private static final UUID INSTITUCION_ID = UUID.randomUUID();
    private static final UUID CUENTA_ID = UUID.randomUUID();

    @Mock
    private CuentaFinancieraRepository cuentas;

    private ValidadorReferenciaCuenta validador;

    @BeforeEach
    void preparar() {
        validador = new ValidadorReferenciaCuenta(cuentas);
    }

    @Test
    void unaReferenciaNuevaSeNormalizaYSeCompruebaSinExclusion() {
        when(cuentas.existeNumeroEnInstitucion(
            INSTITUCION_ID, "123456", null)).thenReturn(false);

        String numero = validador.resolver(
            " 123456 ", INSTITUCION_ID, null);

        assertThat(numero).isEqualTo("123456");
        verify(cuentas).existeNumeroEnInstitucion(
            INSTITUCION_ID, "123456", null);
    }

    @Test
    void unaReferenciaVaciaNoCompruebaDuplicados() {
        String numero = validador.resolver(
            "   ", INSTITUCION_ID, null);

        assertThat(numero).isNull();
        verifyNoInteractions(cuentas);
    }

    @Test
    void alEditarSinReferenciaNuevaSeConservaLaExistente() {
        CuentaFinanciera existente = cuenta("1234");
        when(cuentas.existeNumeroEnInstitucion(
            INSTITUCION_ID, "1234", CUENTA_ID)).thenReturn(false);

        String numero = validador.resolver(null, INSTITUCION_ID, existente);

        assertThat(numero).isEqualTo("1234");
        verify(cuentas).existeNumeroEnInstitucion(
            INSTITUCION_ID, "1234", CUENTA_ID);
    }

    @Test
    void unDuplicadoEnElAltaFalla() {
        when(cuentas.existeNumeroEnInstitucion(
            INSTITUCION_ID, "123456", null)).thenReturn(true);

        assertThatThrownBy(() -> validador.resolver(
            "123456", INSTITUCION_ID, null))
            .isInstanceOf(CuentaDuplicadaException.class);
    }

    @Test
    void unDuplicadoAlEditarExcluyeLaPropiaCuenta() {
        CuentaFinanciera existente = cuenta("1234");
        when(cuentas.existeNumeroEnInstitucion(
            INSTITUCION_ID, "9999", CUENTA_ID)).thenReturn(true);

        assertThatThrownBy(() -> validador.resolver(
            "9999", INSTITUCION_ID, existente))
            .isInstanceOf(CuentaDuplicadaException.class);
    }

    private static CuentaFinanciera cuenta(String numero) {
        return CuentaFinanciera.reconstruir(
            CUENTA_ID,
            UUID.randomUUID(),
            INSTITUCION_ID,
            UUID.randomUUID(),
            "Cuenta existente",
            null,
            new NumeroCuenta(numero),
            UUID.randomUUID(),
            true,
            LocalDate.of(2023, 1, 1),
            null);
    }
}
