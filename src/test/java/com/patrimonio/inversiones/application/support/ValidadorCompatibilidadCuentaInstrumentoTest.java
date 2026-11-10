package com.patrimonio.inversiones.application.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.inversiones.domain.error.PosicionIncompatibleException;
import com.patrimonio.inversiones.domain.error.PosicionInvalidaException;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas conservadoras de compatibilidad cuenta/instrumento.
 */
class ValidadorCompatibilidadCuentaInstrumentoTest {

    private final ValidadorCompatibilidadCuentaInstrumento validador =
        new ValidadorCompatibilidadCuentaInstrumento();

    @Test
    void unaCuentaDeExchangeSoloAdmiteActivosDigitales() {
        assertThatCode(() -> validador.validar(
            cuenta("Cuenta de exchange", true),
            clase("Activos digitales")))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> validador.validar(
            cuenta("Cuenta de exchange", true),
            clase("Renta variable")))
            .isInstanceOf(PosicionIncompatibleException.class)
            .hasMessageContaining("exchange");
    }

    @Test
    void unaCuentaAPlazoNoAdmitePosicionesBursatiles() {
        assertThatThrownBy(() -> validador.validar(
            cuenta("Cuenta a plazo", true),
            clase("Renta fija")))
            .isInstanceOf(PosicionIncompatibleException.class)
            .hasMessageContaining("plazo");
    }

    @Test
    void unaCuentaDeCorretajeAdmiteRentaVariableYFijaPeroNoDigitales() {
        assertThatCode(() -> validador.validar(
            cuenta("Cuenta de corretaje", true),
            clase("Renta variable")))
            .doesNotThrowAnyException();

        assertThatCode(() -> validador.validar(
            cuenta("Cuenta de corretaje", true),
            clase("Renta fija")))
            .doesNotThrowAnyException();

        assertThatThrownBy(() -> validador.validar(
            cuenta("Cuenta de corretaje", true),
            clase("Activos digitales")))
            .isInstanceOf(PosicionIncompatibleException.class)
            .hasMessageContaining("corretaje");
    }

    @Test
    void unaCuentaCorrienteAceptaCualquierClase() {
        assertThatCode(() -> validador.validar(
            cuenta("Cuenta corriente", true),
            clase("Activos digitales")))
            .doesNotThrowAnyException();
    }

    @Test
    void unaCuentaInactivaNoAdmitePosicionesNuevas() {
        assertThatThrownBy(() -> validador.exigirCuentaActiva(
            cuenta("Cuenta corriente", false)))
            .isInstanceOf(PosicionInvalidaException.class)
            .hasMessageContaining("inactiva");
    }

    @Test
    void losNombresConAcentosYMayusculasSeNormalizan() {
        assertThatThrownBy(() -> validador.validar(
            cuenta("Cuenta de INVERSIÓN", true),
            clase("Activos digitales")))
            .isInstanceOf(PosicionIncompatibleException.class);
    }

    private static CuentaResumen cuenta(String tipo, boolean activa) {
        return new CuentaResumen(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Cuenta",
            UUID.randomUUID(),
            UUID.randomUUID(),
            tipo,
            UUID.randomUUID(),
            activa);
    }

    private static ClaseActivo clase(String nombre) {
        return new ClaseActivo(
            UUID.randomUUID(), nombre, null, true);
    }
}
