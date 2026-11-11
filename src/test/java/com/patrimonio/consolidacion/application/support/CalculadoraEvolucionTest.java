package com.patrimonio.consolidacion.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.patrimonio.consolidacion.domain.model.GranularidadEvolucion;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las fechas de corte de las series históricas.
 */
class CalculadoraEvolucionTest {

    @Test
    void laSerieMensualCierraCadaMesYTerminaEnLaFechaFinal() {
        assertThat(CalculadoraEvolucion.fechas(
            GranularidadEvolucion.MENSUAL,
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 3, 10)))
            .containsExactly(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 10));
    }

    @Test
    void laSerieSemanalAgregaElCierreCuandoNoCoincide() {
        assertThat(CalculadoraEvolucion.fechas(
            GranularidadEvolucion.SEMANAL,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 20)))
            .containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 8),
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 1, 20));
    }

    @Test
    void unRangoInvertidoNoProduceFechas() {
        assertThat(CalculadoraEvolucion.fechas(
            GranularidadEvolucion.DIARIA,
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 1, 1)))
            .isEmpty();
    }
}
