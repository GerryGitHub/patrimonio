package com.patrimonio.consolidacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.consolidacion.domain.error.SnapshotInvalidoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del snapshot patrimonial manual.
 */
class SnapshotPatrimonioTest {

    private final UUID id = UUID.randomUUID();
    private final UUID portafolioId = UUID.randomUUID();
    private final UUID monedaId = UUID.randomUUID();
    private final LocalDate fecha = LocalDate.of(2026, 5, 1);
    private final Instant ahora = Instant.parse("2026-05-01T12:00:00Z");

    @Test
    void crearDejaNulosLosCamposSinFuenteConfiable() {
        SnapshotPatrimonio snapshot = SnapshotPatrimonio.crear(
            id,
            portafolioId,
            fecha,
            monedaId,
            new BigDecimal("1000"),
            new BigDecimal("25"),
            SnapshotPatrimonio.COBERTURA_INVERSIONES,
            SnapshotPatrimonio.LIMITACIONES_POR_DEFECTO,
            ahora);

        assertThat(snapshot.capitalInvertido()).isEqualByComparingTo("1000");
        assertThat(snapshot.resultadoRealizado()).isEqualByComparingTo("25");
        assertThat(snapshot.efectivo()).isNull();
        assertThat(snapshot.pasivos()).isNull();
        assertThat(snapshot.valorMercado()).isNull();
        assertThat(snapshot.patrimonioNeto()).isNull();
        assertThat(snapshot.fuente()).isEqualTo(SnapshotPatrimonio.FUENTE_MANUAL);
        assertThat(snapshot.cobertura())
            .isEqualTo(SnapshotPatrimonio.COBERTURA_INVERSIONES);
    }

    @Test
    void unCapitalNegativoNoEsUnSnapshotValido() {
        assertThatThrownBy(() -> SnapshotPatrimonio.crear(
            id,
            portafolioId,
            fecha,
            monedaId,
            new BigDecimal("-1"),
            BigDecimal.ZERO,
            SnapshotPatrimonio.COBERTURA_INVERSIONES,
            null,
            ahora))
            .isInstanceOf(SnapshotInvalidoException.class)
            .hasMessageContaining("negativo");
    }

    @Test
    void laCoberturaEsObligatoria() {
        assertThatThrownBy(() -> SnapshotPatrimonio.crear(
            id,
            portafolioId,
            fecha,
            monedaId,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "  ",
            null,
            ahora))
            .isInstanceOf(SnapshotInvalidoException.class)
            .hasMessageContaining("cobertura");
    }
}
