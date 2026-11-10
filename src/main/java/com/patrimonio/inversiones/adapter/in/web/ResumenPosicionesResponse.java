package com.patrimonio.inversiones.adapter.in.web;

import java.util.List;
import java.util.UUID;

/**
 * DTO de los agregados reales de posiciones de un portafolio.
 *
 * <p>No representa valor de mercado, rendimiento ni patrimonio actualizado.
 * Los importes se agrupan por moneda y viajan como cadena.</p>
 */
public record ResumenPosicionesResponse(
    int totalPosiciones,
    int posicionesActivas,
    int instrumentosDistintos,
    List<CostoPorMonedaResponse> costoPorMoneda,
    List<DistribucionCategoriaResponse> distribucionPorCategoria
) {

    public record CostoPorMonedaResponse(
        UUID monedaId,
        String codigo,
        String nombre,
        String simbolo,
        int decimales,
        String costo
    ) {
    }

    public record DistribucionCategoriaResponse(
        UUID claseActivoId,
        String claseActivoNombre,
        int posiciones,
        int instrumentos,
        List<CostoPorMonedaResponse> costoPorMoneda
    ) {
    }
}
