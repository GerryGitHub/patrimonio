package com.patrimonio.acceso.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code usuario}.
 *
 * <p>Solo se usa para autenticar y para comprobar que la cuenta sigue activa.
 * El hash de la contraseña nunca sale de esta capa hacia HTTP.</p>
 */
@Entity
@Table(name = "usuario", schema = "public")
public class UsuarioEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected UsuarioEntity() {
        // Constructor requerido por JPA.
    }

    /**
     * Crea una cuenta activa con las fechas de auditoría actuales.
     *
     * <p>El identificador se genera en la aplicación para poder devolverlo sin
     * depender de una lectura posterior. El hash ya debe venir calculado.</p>
     */
    public static UsuarioEntity nuevo(
        UUID id,
        String nombre,
        String email,
        String passwordHash
    ) {
        UsuarioEntity usuario = new UsuarioEntity();
        Instant ahora = Instant.now();
        usuario.id = id;
        usuario.nombre = nombre;
        usuario.email = email;
        usuario.passwordHash = passwordHash;
        usuario.activo = true;
        usuario.fechaCreacion = ahora;
        usuario.fechaActualizacion = ahora;
        return usuario;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActivo() {
        return activo;
    }
}
