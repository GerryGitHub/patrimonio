package com.patrimonio.acceso.application.port.out;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para consultar el catálogo de monedas.
 *
 * <p>Solo expone monedas activas: una moneda dada de baja no puede usarse como
 * base de un portafolio nuevo.</p>
 */
public interface MonedaQueryPort {

    List<MonedaResumen> listarActivas();

    Optional<MonedaResumen> buscarActivaPorId(UUID monedaId);
}
