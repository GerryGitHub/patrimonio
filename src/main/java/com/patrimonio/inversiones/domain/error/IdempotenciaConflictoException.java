package com.patrimonio.inversiones.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * La clave de idempotencia ya fue usada para otra posición o con otros datos.
 */
public class IdempotenciaConflictoException extends DomainException {

    public IdempotenciaConflictoException(String mensaje) {
        super("IDEMPOTENCIA_CONFLICTO", mensaje);
    }
}
