package com.patrimonio.acceso.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code membresia}.
 *
 * <p>El rol se conserva como texto porque el esquema lo define así. El alta de
 * portafolios crea la membresía {@code PROPIETARIO} del usuario que los crea;
 * la capacidad de lectura la conceden todos los roles admitidos por el
 * esquema.</p>
 */
@Entity
@Table(name = "membresia", schema = "public")
public class MembresiaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "portafolio_id", nullable = false, updatable = false)
    private UUID portafolioId;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected MembresiaEntity() {
        // Constructor requerido por JPA.
    }

    public MembresiaEntity(
        UUID id,
        UUID usuarioId,
        UUID portafolioId,
        String rol,
        Instant fechaCreacion
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.portafolioId = portafolioId;
        this.rol = rol;
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getPortafolioId() {
        return portafolioId;
    }

    public String getRol() {
        return rol;
    }
}
