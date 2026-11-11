package com.patrimonio.inversiones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.inversiones.domain.error.PosicionInvalidaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del agregado de posición sin Spring ni base de datos.
 */
class PosicionTest {

    private static final UUID ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUENTA_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INSTRUMENTO_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MONEDA_ID =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant AHORA = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant APERTURA = Instant.parse("2026-01-10T00:00:00Z");

    @Test
    void crear_calculaElCostoTotal() {
        Posicion posicion = crear(
            new BigDecimal("10.5"), new BigDecimal("25.75"));

        assertThat(posicion.costoTotal())
            .isEqualByComparingTo(new BigDecimal("270.375"));
        assertThat(posicion.activo()).isTrue();
        assertThat(posicion.fechaApertura()).isEqualTo(APERTURA);
    }

    @Test
    void crear_conCantidadNegativa_falla() {
        assertThatThrownBy(() -> crear(
            new BigDecimal("-1"), new BigDecimal("10")))
            .isInstanceOf(PosicionInvalidaException.class)
            .hasMessageContaining("cantidad");
    }

    @Test
    void crear_conCostoNegativo_falla() {
        assertThatThrownBy(() -> crear(
            new BigDecimal("1"), new BigDecimal("-10")))
            .isInstanceOf(PosicionInvalidaException.class)
            .hasMessageContaining("costo unitario");
    }

    @Test
    void crear_conFechaFutura_falla() {
        assertThatThrownBy(() -> Posicion.crear(
            ID,
            CUENTA_ID,
            INSTRUMENTO_ID,
            BigDecimal.ONE,
            BigDecimal.TEN,
            MONEDA_ID,
            null,
            AHORA.plusSeconds(86_400),
            AHORA))
            .isInstanceOf(PosicionInvalidaException.class)
            .hasMessageContaining("fecha de apertura");
    }

    @Test
    void actualizarDatos_conservaCuentaEInstrumento() {
        Posicion original = crear(
            new BigDecimal("10"), new BigDecimal("20"));

        Posicion actualizada = original.actualizarDatos(
            new BigDecimal("12"),
            new BigDecimal("21.5"),
            "  Corregida  ",
            APERTURA.plusSeconds(3_600),
            AHORA.plusSeconds(60));

        assertThat(actualizada.id()).isEqualTo(original.id());
        assertThat(actualizada.cuentaId()).isEqualTo(CUENTA_ID);
        assertThat(actualizada.instrumentoId()).isEqualTo(INSTRUMENTO_ID);
        assertThat(actualizada.monedaId()).isEqualTo(MONEDA_ID);
        assertThat(actualizada.cantidad()).isEqualByComparingTo("12");
        assertThat(actualizada.costoUnitario()).isEqualByComparingTo("21.5");
        assertThat(actualizada.costoTotal()).isEqualByComparingTo("258");
        assertThat(actualizada.observaciones()).contains("  Corregida  ");
    }

    @Test
    void cambiarEstado_conservaLosImportes() {
        Posicion original = crear(
            new BigDecimal("10"), new BigDecimal("20"));

        Posicion desactivada = original.cambiarEstado(
            false, AHORA.plusSeconds(60));

        assertThat(desactivada.activo()).isFalse();
        assertThat(desactivada.cantidad()).isEqualByComparingTo("10");
        assertThat(desactivada.costoUnitario()).isEqualByComparingTo("20");
    }

    @Test
    void aplicar_actualizaCantidadCostoYResultado() {
        Posicion original = crear(
            new BigDecimal("10"), new BigDecimal("20"));

        Posicion actualizada = original.aplicar(
            new ResultadoCalculo(
                new BigDecimal("40"),
                new BigDecimal("38"),
                new BigDecimal("40"),
                new BigDecimal("-2"),
                new BigDecimal("-2"),
                new BigDecimal("-40"),
                new BigDecimal("8"),
                new BigDecimal("160")),
            Instant.parse("2026-01-12T00:00:00Z"),
            AHORA.plusSeconds(120));

        assertThat(actualizada.cantidad()).isEqualByComparingTo("8");
        assertThat(actualizada.costoTotal()).isEqualByComparingTo("160");
        assertThat(actualizada.costoUnitario()).isEqualByComparingTo("20");
        assertThat(actualizada.resultadoRealizado()).isEqualByComparingTo("-2");
        assertThat(actualizada.version()).isEqualTo(1);
        assertThat(actualizada.fechaUltimoMovimiento())
            .contains(Instant.parse("2026-01-12T00:00:00Z"));
    }

    @Test
    void aplicar_conMovimientoAnterior_conservaLaFechaMasReciente() {
        Posicion original = crear(
            new BigDecimal("10"), new BigDecimal("20"));

        Posicion actualizada = original.aplicar(
            new ResultadoCalculo(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                BigDecimal.ONE,
                new BigDecimal("20"),
                new BigDecimal("11"),
                new BigDecimal("220")),
            Instant.parse("2026-01-05T00:00:00Z"),
            AHORA.plusSeconds(120));

        assertThat(actualizada.fechaUltimoMovimiento()).contains(APERTURA);
    }

    @Test
    void reparar_reemplazaElEstadoMaterializadoSinTocarLaIdentidad() {
        Posicion original = crear(
            new BigDecimal("10"), new BigDecimal("20"));

        Posicion reparada = original.reparar(
            new BigDecimal("7"),
            new BigDecimal("175"),
            new BigDecimal("5"),
            Instant.parse("2026-01-14T00:00:00Z"),
            AHORA.plusSeconds(300));

        assertThat(reparada.id()).isEqualTo(original.id());
        assertThat(reparada.cantidad()).isEqualByComparingTo("7");
        assertThat(reparada.costoTotal()).isEqualByComparingTo("175");
        assertThat(reparada.costoUnitario()).isEqualByComparingTo("25");
        assertThat(reparada.resultadoRealizado()).isEqualByComparingTo("5");
        assertThat(reparada.version()).isEqualTo(1);
    }

    private static Posicion crear(
        BigDecimal cantidad,
        BigDecimal costoUnitario
    ) {
        return Posicion.crear(
            ID,
            CUENTA_ID,
            INSTRUMENTO_ID,
            cantidad,
            costoUnitario,
            MONEDA_ID,
            "Observación",
            APERTURA,
            AHORA);
    }
}
