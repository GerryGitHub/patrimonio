package com.patrimonio.cuentas.domain.model;

import com.patrimonio.cuentas.domain.error.TipoCuentaInvalidoException;
import java.util.Optional;
import java.util.UUID;

/**
 * Tipo de cuenta del catálogo reutilizable.
 *
 * <p>Es un catálogo de solo lectura para el módulo: el alta se realiza con los
 * datos iniciales y la aplicación no lo modifica.</p>
 */
public final class TipoCuenta {

    private static final int LONGITUD_MAXIMA_NOMBRE = 100;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 255;

    private final UUID id;
    private final String nombre;
    private final Optional<String> descripcion;
    private final boolean activo;

    private TipoCuenta(
        UUID id,
        String nombre,
        String descripcion,
        boolean activo
    ) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = Optional.ofNullable(descripcion);
        this.activo = activo;
    }

    public static TipoCuenta reconstruir(
        UUID id,
        String nombre,
        String descripcion,
        boolean activo
    ) {
        if (id == null) {
            throw new TipoCuentaInvalidoException(
                "El identificador del tipo de cuenta es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new TipoCuentaInvalidoException(
                "El nombre del tipo de cuenta es obligatorio.");
        }
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new TipoCuentaInvalidoException(
                "El nombre del tipo de cuenta no puede superar "
                    + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
        if (descripcion != null && descripcion.length() > LONGITUD_MAXIMA_DESCRIPCION) {
            throw new TipoCuentaInvalidoException(
                "La descripción del tipo de cuenta no puede superar "
                    + LONGITUD_MAXIMA_DESCRIPCION + " caracteres.");
        }
        return new TipoCuenta(id, nombre, descripcion, activo);
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public Optional<String> descripcion() {
        return descripcion;
    }

    public boolean activo() {
        return activo;
    }
}
