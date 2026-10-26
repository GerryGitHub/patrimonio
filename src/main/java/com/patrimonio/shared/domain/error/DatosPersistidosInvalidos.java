package com.patrimonio.shared.domain.error;

/**
 * Los datos leídos de la base no permiten reconstruir un agregado válido.
 *
 * <p>Indica una incoherencia entre el esquema, los datos almacenados y las
 * invariantes del dominio. No se corrige en silencio: se falla de forma
 * explícita para que la incidencia se investigue.</p>
 */
public class DatosPersistidosInvalidos extends DomainException {

    public DatosPersistidosInvalidos(String mensaje) {
        super("DATOS_INCONSISTENTES", mensaje);
    }

    public DatosPersistidosInvalidos(String mensaje, Throwable causa) {
        super("DATOS_INCONSISTENTES", mensaje, causa);
    }
}
