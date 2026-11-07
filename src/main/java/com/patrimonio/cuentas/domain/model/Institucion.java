package com.patrimonio.cuentas.domain.model;

import com.patrimonio.cuentas.domain.error.InstitucionInvalidaException;
import java.util.Optional;
import java.util.UUID;

/**
 * Institución financiera del catálogo.
 *
 * <p>Agregado de dominio puro. Una institución puede ser reutilizada por
 * varias cuentas y por cuentas de distintos portafolios; no pertenece a un
 * usuario concreto.</p>
 */
public final class Institucion {

    private static final int LONGITUD_MAXIMA_NOMBRE = 150;
    private static final int LONGITUD_MAXIMA_PAIS = 100;
    private static final int LONGITUD_MAXIMA_SITIO_WEB = 255;

    private final UUID id;
    private final String nombre;
    private final TipoInstitucion tipo;
    private final Optional<String> pais;
    private final Optional<String> sitioWeb;
    private final boolean activo;

    private Institucion(
        UUID id,
        String nombre,
        TipoInstitucion tipo,
        String pais,
        String sitioWeb,
        boolean activo
    ) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.pais = Optional.ofNullable(pais);
        this.sitioWeb = Optional.ofNullable(sitioWeb);
        this.activo = activo;
    }

    /**
     * Crea una institución nueva y activa.
     */
    public static Institucion crear(
        UUID id,
        String nombre,
        TipoInstitucion tipo,
        String pais,
        String sitioWeb
    ) {
        validar(id, nombre, tipo, pais, sitioWeb);
        return new Institucion(id, nombre, tipo, pais, sitioWeb, true);
    }

    /**
     * Reconstruye una institución desde persistencia.
     */
    public static Institucion reconstruir(
        UUID id,
        String nombre,
        TipoInstitucion tipo,
        String pais,
        String sitioWeb,
        boolean activo
    ) {
        validar(id, nombre, tipo, pais, sitioWeb);
        return new Institucion(id, nombre, tipo, pais, sitioWeb, activo);
    }

    private static void validar(
        UUID id,
        String nombre,
        TipoInstitucion tipo,
        String pais,
        String sitioWeb
    ) {
        if (id == null) {
            throw new InstitucionInvalidaException(
                "El identificador de la institución es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new InstitucionInvalidaException(
                "El nombre de la institución es obligatorio.");
        }
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new InstitucionInvalidaException(
                "El nombre de la institución no puede superar "
                    + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
        if (tipo == null) {
            throw new InstitucionInvalidaException(
                "La categoría de la institución es obligatoria.");
        }
        if (pais != null && pais.length() > LONGITUD_MAXIMA_PAIS) {
            throw new InstitucionInvalidaException(
                "El país no puede superar " + LONGITUD_MAXIMA_PAIS + " caracteres.");
        }
        if (sitioWeb != null && sitioWeb.length() > LONGITUD_MAXIMA_SITIO_WEB) {
            throw new InstitucionInvalidaException(
                "El sitio web no puede superar "
                    + LONGITUD_MAXIMA_SITIO_WEB + " caracteres.");
        }
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public TipoInstitucion tipo() {
        return tipo;
    }

    public Optional<String> pais() {
        return pais;
    }

    public Optional<String> sitioWeb() {
        return sitioWeb;
    }

    public boolean activo() {
        return activo;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Institucion institucion && id.equals(institucion.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
