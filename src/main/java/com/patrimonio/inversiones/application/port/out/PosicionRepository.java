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
     */
    List<Posicion> listarPorCuentas(
        Collection<UUID> cuentaIds,
        UUID cuentaId,
        UUID instrumentoId,
        Collection<UUID> instrumentoIdsRestringidos,
        Boolean activo);

    Optional<Posicion> buscarPorId(UUID posicionId);

    /**
     * Bloquea la posición para actualizarla en la misma transacción que el
     * movimiento.
     */
    Optional<Posicion> buscarPorIdParaActualizar(UUID posicionId);

    Posicion guardar(Posicion posicion);
}
