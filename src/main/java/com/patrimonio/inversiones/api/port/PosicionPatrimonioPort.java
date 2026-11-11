package com.patrimonio.inversiones.api.port;

import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Contrato público de consulta de posiciones para la consolidación.
 *
 * <p>El consumidor debe haber autorizado previamente las cuentas. Este puerto
 * no conoce usuarios ni membresías y nunca cruza portafolios por sí mismo.</p>
 */
public interface PosicionPatrimonioPort {

    /**
     * Posiciones de las cuentas indicadas.
     *
     * @param cuentaIds cuentas autorizadas
     * @param soloActivas si es {@code true}, omite las posiciones cerradas
     */
    List<PosicionPatrimonio> listarPorCuentas(
        Collection<UUID> cuentaIds,
        boolean soloActivas);
}
