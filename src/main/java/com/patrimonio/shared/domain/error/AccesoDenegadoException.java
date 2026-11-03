package com.patrimonio.shared.domain.error;

/**
 * El usuario está autenticado, pero su membresía no concede la capacidad
 * solicitada.
 *
 * <p>No se usa para ocultar la existencia de un recurso: en ese caso se lanza
 * {@link NotFoundException}. El adaptador REST lo traduce a
 * {@code 403 Forbidden}.</p>
 */
public class AccesoDenegadoException extends DomainException {

    public AccesoDenegadoException() {
        super("ACCESO_DENEGADO", "No tiene permiso para realizar esta operación.");
    }
}
