package com.patrimonio.inversiones.application.port.out;

import com.patrimonio.inversiones.domain.model.Posicion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de posiciones.
 *
 * <p>Las posiciones se consultan por las cuentas de un portafolio. La
 * aplicación obtiene primero las cuentas autorizadas mediante el contrato
 * público del módulo {@code cuentas}; este puerto nunca cruza portafolios por
 * sí mismo.</p>
 */
public interface PosicionRepository {

    /**
     * Posiciones de un conjunto de cuentas con filtros opcionales.
     *
     * @param cuentaIds cuentas autorizadas del portafolio
     * @param cuentaId filtro opcional por una cuenta concreta
     * @param instrumentoId filtro opcional por instrumento
     * @param instrumentoIdsRestringidos cuando no es {@code null}, limita el
     *     resultado a esos instrumentos (se usa para filtrar por clase de
     *     activo)
     * @param activo filtro opcional por estado
     */
    List<Posicion> listarPorCuentas(
        Collection<UUID> cuentaIds,
        UUID cuentaId,
        UUID instrumentoId,
        Collection<UUID> instrumentoIdsRestringidos,
        Boolean activo);

    Optional<Posicion> buscarPorId(UUID posicionId);

    Posicion guardar(Posicion posicion);
}
