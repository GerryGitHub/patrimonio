package com.patrimonio.shared.domain.error;

/**
 * El recurso solicitado no existe.
 *
 * <p>Es un resultado de negocio normal, no un fallo de infraestructura. El
 * adaptador REST lo traduce a {@code 404 Not Found} con el código
 * {@code NO_ENCONTRADO}.</p>
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String mensaje) {
        super("NO_ENCONTRADO", mensaje);
    }
}
