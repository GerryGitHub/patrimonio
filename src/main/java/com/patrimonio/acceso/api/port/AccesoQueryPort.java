package com.patrimonio.acceso.api.port;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consulta de autorización por membresía para otros módulos.
 *
 * <p>La comprobación de acceso pertenece a la capa de aplicación del módulo
 * consumidor: este puerto solo responde qué permiso tiene un usuario sobre un
 * portafolio. Devuelve {@link Optional#empty()} cuando no existe membresía,
 * el portafolio no existe, está inactivo o el usuario está inactivo.</p>
 */
public interface AccesoQueryPort {

    /**
     * Permiso del usuario sobre el portafolio indicado.
     */
    Optional<PermisoPortafolio> buscarPermiso(UUID usuarioId, UUID portafolioId);

    /**
     * Portafolios activos a los que el usuario tiene acceso, ordenados por
     * nombre e identificador.
     */
    List<PortafolioResumen> listarAutorizados(UUID usuarioId);
}
