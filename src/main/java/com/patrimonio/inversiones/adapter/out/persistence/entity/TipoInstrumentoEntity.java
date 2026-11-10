package com.patrimonio.inversiones.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code tipo_instrumento}.
 */
@Entity
@Table(name = "tipo_instrumento", schema = "public")
public class TipoInstrumentoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "clase_activo_id", nullable = false, updatable = false)
    private UUID claseActivoId;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected TipoInstrumentoEntity() {
        // Constructor requerido por JPA.
    }

    public UUID getId() {
        return id;
    }

    public UUID getClaseActivoId() {
        return claseActivoId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }
}
