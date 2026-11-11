package com.patrimonio.inversiones.application.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Agregados reales de las posiciones de un portafolio.
 *
 * <p>No contiene valor de mercado, rendimiento ni patrimonio actualizado. El
 * costo y el resultado se agrupan por moneda porque una posición puede estar
 * denominada en una moneda distinta de la moneda base del portafolio; no se
 * suman monedas distintas sin conversión.</p>
 */
public record ResumenPosicionesView(
    int totalPosiciones,
    int posicionesActivas,
    int instrumentosDistintos,
    List<CostoPorMonedaView> costoPorMoneda,
    List<ResultadoPorMonedaView> resultadoPorMoneda,
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
     * Resultado realizado agregado de un conjunto de posiciones en una moneda
     * concreta. Puede ser negativo y no incluye resultados no realizados.
     */
    public record ResultadoPorMonedaView(
        UUID monedaId,
        String codigo,
        String nombre,
        String simbolo,
        int decimales,
        BigDecimal resultado
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
