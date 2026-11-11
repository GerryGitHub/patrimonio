package com.patrimonio.inversiones.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Mapeo JPA del catálogo {@code tipo_movimiento}.
 */
@Entity
@Table(name = "tipo_movimiento", schema = "public")
public class TipoMovimientoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "categoria", length = 50)
    private String categoria;

    @Column(name = "afecta_posicion", nullable = false)
    private boolean afectaPosicion;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected TipoMovimientoEntity() {
        // Constructor requerido por JPA.
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public boolean isAfectaPosicion() {
        return afectaPosicion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }
}
