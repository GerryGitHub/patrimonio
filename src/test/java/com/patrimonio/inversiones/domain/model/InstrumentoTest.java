package com.patrimonio.inversiones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.inversiones.domain.error.InstrumentoInvalidoException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del agregado de instrumento sin Spring ni base de datos.
 */
class InstrumentoTest {

    private static final UUID ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TIPO_ID =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MONEDA_ID =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant AHORA = Instant.parse("2026-01-15T12:00:00Z");

    @Test
    void crear_normalizaElIsinYActivaElInstrumento() {
        Instrumento instrumento = crear(" apple inc ", "aapl", " us0378331005 ");

        assertThat(instrumento.nombre()).isEqualTo(" apple inc ");
        assertThat(instrumento.simbolo()).contains("aapl");
        assertThat(instrumento.isin()).contains("US0378331005");
        assertThat(instrumento.activo()).isTrue();
        assertThat(instrumento.fechaCreacion()).isEqualTo(AHORA);
        assertThat(instrumento.fechaActualizacion()).isEqualTo(AHORA);
    }

    @Test
    void crear_conIsinVacio_loTrataComoAusente() {
        Instrumento instrumento = crear("Apple", "AAPL", "   ");

        assertThat(instrumento.isin()).isEmpty();
    }

    @Test
    void crear_conIsinInvalido_falla() {
        assertThatThrownBy(() -> crear("Apple", "AAPL", "US037833100"))
            .isInstanceOf(InstrumentoInvalidoException.class)
            .hasMessageContaining("ISIN");

        assertThatThrownBy(() -> crear("Apple", "AAPL", "US03783310-5"))
            .isInstanceOf(InstrumentoInvalidoException.class)
            .hasMessageContaining("ISIN");
    }

    @Test
    void crear_conNombreEnBlanco_falla() {
        assertThatThrownBy(() -> crear("   ", "AAPL", null))
            .isInstanceOf(InstrumentoInvalidoException.class)
            .hasMessageContaining("nombre");
    }

    @Test
    void crear_conSimboloDemasiadoLargo_falla() {
        assertThatThrownBy(() -> crear("Apple", "A".repeat(33), null))
            .isInstanceOf(InstrumentoInvalidoException.class)
            .hasMessageContaining("símbolo");
    }

    @Test
    void actualizarDatos_conservaIdentidadYEstado() {
        Instrumento original = crear("Apple", "AAPL", "US0378331005");

        Instrumento actualizado = original.actualizarDatos(
            TIPO_ID,
            "Apple Inc.",
            "AAPL",
            "Apple",
            "NASDAQ",
            MONEDA_ID,
            "PROV-1",
            "US0378331005",
            "Descripción",
            AHORA.plusSeconds(60));

        assertThat(actualizado.id()).isEqualTo(original.id());
        assertThat(actualizado.activo()).isTrue();
        assertThat(actualizado.nombre()).isEqualTo("Apple Inc.");
        assertThat(actualizado.emisor()).contains("Apple");
        assertThat(actualizado.mercado()).contains("NASDAQ");
        assertThat(actualizado.fechaCreacion()).isEqualTo(AHORA);
        assertThat(actualizado.fechaActualizacion())
            .isEqualTo(AHORA.plusSeconds(60));
    }

    @Test
    void cambiarEstado_conservaLosDatosYActualizaLaFecha() {
        Instrumento original = crear("Apple", "AAPL", "US0378331005");

        Instrumento desactivado = original.cambiarEstado(
            false, AHORA.plusSeconds(120));

        assertThat(desactivado.activo()).isFalse();
        assertThat(desactivado.nombre()).isEqualTo(original.nombre());
        assertThat(desactivado.fechaActualizacion())
            .isEqualTo(AHORA.plusSeconds(120));
    }

    private static Instrumento crear(
        String nombre,
        String simbolo,
        String isin
    ) {
        return Instrumento.crear(
            ID,
            TIPO_ID,
            nombre,
            simbolo,
            "Apple",
            "NASDAQ",
            MONEDA_ID,
            "PROV-1",
            isin,
            "Descripción",
            AHORA);
    }
}
