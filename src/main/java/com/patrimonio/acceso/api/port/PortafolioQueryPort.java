package com.patrimonio.acceso.api.port;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta de solo lectura de portafolios para otros módulos.
 *
 * <p>Devuelve {@link Optional#empty()} cuando el portafolio no existe. Solo
 * lanza excepciones ante fallos de infraestructura.</p>
 */
public interface PortafolioQueryPort {

    Optional<PortafolioResumen> buscarPorId(UUID portafolioId);
}
