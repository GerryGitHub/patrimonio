package com.patrimonio.consolidacion.domain.error;

import com.patrimonio.shared.domain.error.DomainException;

/**
 * El snapshot patrimonial no cumple las reglas del dominio.
 */
public class SnapshotInvalidoException extends DomainException {

    public SnapshotInvalidoException(String mensaje) {
        super("SNAPSHOT_INVALIDO", mensaje);
    }
}
