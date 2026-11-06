package com.patrimonio.acceso.application.port.out;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.UUID;

/**
 * Puerto de salida para persistir un portafolio nuevo y la membresía inicial
 * de su creador.
 *
 * <p>La implementación debe ejecutar ambas inserciones en la transacción del
 * caso de uso: un portafolio sin membresía no sería consultable por nadie.</p>
 */
public interface PortafolioRegistroPort {

    PortafolioResumen crear(
        UUID usuarioId,
        String nombre,
        String descripcion,
        MonedaResumen monedaBase);
}
