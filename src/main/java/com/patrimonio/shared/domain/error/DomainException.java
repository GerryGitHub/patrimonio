package com.patrimonio.shared.domain.error;

/**
 * Excepción base para los errores de negocio del dominio.
 *
 * <p>No conoce HTTP ni Spring. Cada excepción concreta aporta un código estable
 * que el adaptador REST traduce a un {@code ProblemDetail}.</p>
 */
public class DomainException extends RuntimeException {

    private final String codigo;

    protected DomainException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    protected DomainException(String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }

    /**
     * Código estable del error, independiente del texto del mensaje.
     */
    public String codigo() {
        return codigo;
    }
}
