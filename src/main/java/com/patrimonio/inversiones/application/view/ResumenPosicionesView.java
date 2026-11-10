package com.patrimonio.inversiones.application.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Agregados reales de las posiciones de un portafolio.
 *
 * <p>No contiene valor de mercado, rendimiento ni patrimonio actualizado. El
 * costo se agrupa por moneda porque una posición puede estar denominada en una
 * moneda distinta de la moneda base del portafolio; no se suman monedas
 * distintas sin conversión.</p>
 */
public record ResumenPosicionesView(
    int totalPosiciones,
    int posicionesActivas,
    int instrumentosDistintos,
    List<CostoPorMonedaView> costoPorMoneda,
    List<DistribucionCategoriaView> distribucionPorCategoria
) {

    /**
     * Costo agregado de un conjunto de posiciones en una moneda concreta.
     */
    public record CostoPorMonedaView(
        UUID monedaId,
        String codigo,
        String nombre,
        String simbolo,
        int decimales,
        BigDecimal costo
    ) {
    }

    /**
     * Distribución de posiciones e instrumentos por clase de activo.
     */
    public record DistribucionCategoriaView(
        UUID claseActivoId,
        String claseActivoNombre,
        int posiciones,
        int instrumentos,
        List<CostoPorMonedaView> costoPorMoneda
    ) {
    }
}
