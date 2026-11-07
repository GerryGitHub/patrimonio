package com.patrimonio.acceso.api.port;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público de solo lectura del catálogo de monedas.
 *
 * <p>Otros módulos lo usan para validar la moneda de sus entidades y para
 * resolver los datos que necesitan mostrar, sin acoplarse a la persistencia
 * del módulo {@code acceso}.</p>
 */
public interface MonedaCatalogoPort {

    /**
     * Monedas activas ordenadas por código.
     */
    List<MonedaResumen> listarActivas();

    /**
     * Moneda activa por identificador, si existe.
     */
    Optional<MonedaResumen> buscarActivaPorId(UUID monedaId);

    /**
     * Moneda por identificador, esté activa o no.
     *
     * <p>Se usa para resolver la representación de entidades históricas que
     * conservan una moneda dada de baja.</p>
     */
    Optional<MonedaResumen> buscarPorId(UUID monedaId);
}
