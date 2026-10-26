package com.patrimonio.acceso.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code portafolio}.
 *
 * <p>Esta entidad pertenece al módulo {@code acceso}. En esta fase solo se usa
 * para la consulta de existencia que necesita {@code cuentas}; no se implementan
 * casos de uso de escritura de portafolios.</p>
 */
@Entity
@Table(name = "portafolio", schema = "public")
public class PortafolioEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "moneda_base_id", nullable = false, updatable = false)
    private UUID monedaBaseId;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected PortafolioEntity() {
        // Constructor requerido por JPA.
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public UUID getMonedaBaseId() {
        return monedaBaseId;
    }

    public boolean isActivo() {
        return activo;
    }
}
