package com.patrimonio.acceso.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code moneda}.
 *
 * <p>Entidad anémica de solo lectura. El catálogo lo administra la base de
 * datos; la aplicación no crea ni modifica monedas en esta fase.</p>
 */
@Entity
@Table(name = "moneda", schema = "public")
public class MonedaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 10)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "simbolo", length = 10)
    private String simbolo;

    @Column(name = "decimales", nullable = false)
    private int decimales;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected MonedaEntity() {
        // Constructor requerido por JPA.
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public int getDecimales() {
        return decimales;
    }

    public boolean isActivo() {
        return activo;
    }
}
