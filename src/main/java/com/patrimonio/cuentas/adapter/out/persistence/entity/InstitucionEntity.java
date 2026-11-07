package com.patrimonio.cuentas.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code institucion}.
 *
 * <p>Entidad anémica: solo datos. La categoría se conserva como texto porque
 * el esquema la define como {@code varchar(50)}.</p>
 */
@Entity
@Table(name = "institucion", schema = "public")
public class InstitucionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "tipo", length = 50)
    private String tipo;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "sitio_web", length = 255)
    private String sitioWeb;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected InstitucionEntity() {
        // Constructor requerido por JPA.
    }

    public InstitucionEntity(
        UUID id,
        String nombre,
        String tipo,
        String pais,
        String sitioWeb,
        boolean activo
    ) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.pais = pais;
        this.sitioWeb = sitioWeb;
        this.activo = activo;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getPais() {
        return pais;
    }

    public String getSitioWeb() {
        return sitioWeb;
    }

    public boolean isActivo() {
        return activo;
    }
}
