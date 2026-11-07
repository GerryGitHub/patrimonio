package com.patrimonio.acceso.application.port.out;

import com.patrimonio.acceso.api.port.MonedaCatalogoPort;

/**
 * Puerto de salida para consultar el catálogo de monedas.
 *
 * <p>Extiende el contrato público {@link MonedaCatalogoPort} para que el
 * adaptador de persistencia sea el único que conoce el catálogo y otros
 * módulos puedan reutilizar la misma capacidad sin depender de esta capa.</p>
 */
public interface MonedaQueryPort extends MonedaCatalogoPort {
}
