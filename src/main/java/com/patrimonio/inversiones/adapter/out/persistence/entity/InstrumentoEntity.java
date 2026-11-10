package com.patrimonio.inversiones.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code instrumento}.
 *
 * <p>Entidad anémica: no tiene asociaciones JPA. La referencia al tipo de
 * instrumento y a la moneda se mantiene como UUID. El ISIN no vive aquí: se
 * persiste en {@code identificador_instrumento} mediante
 * {@link IdentificadorInstrumentoEntity}.</p>
 */
@Entity
@Table(name = "instrumento", schema = "public")
public class InstrumentoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tipo_instrumento_id", nullable = false, updatable = false)
    private UUID tipoInstrumentoId;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "simbolo", length = 32)
    private String simbolo;

    @Column(name = "emisor", length = 150)
    private String emisor;

    @Column(name = "mercado", length = 100)
    private String mercado;

    @Column(name = "moneda_id", nullable = false)
    private UUID monedaId;

    @Column(name = "identificador_externo", length = 100)
    private String identificadorExterno;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected InstrumentoEntity() {
        // Constructor requerido por JPA.
    }

    public InstrumentoEntity(
        UUID id,
        UUID tipoInstrumentoId,
        String nombre,
        String simbolo,
        String emisor,
        String mercado,
        UUID monedaId,
        String identificadorExterno,
        String descripcion,
        boolean activo,
        Instant fechaCreacion,
        Instant fechaActualizacion
    ) {
        this.id = id;
        this.tipoInstrumentoId = tipoInstrumentoId;
        this.nombre = nombre;
        this.simbolo = simbolo;
        this.emisor = emisor;
        this.mercado = mercado;
        this.monedaId = monedaId;
        this.identificadorExterno = identificadorExterno;
        this.descripcion = descripcion;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public void actualizar(
        UUID tipoInstrumentoId,
        String nombre,
        String simbolo,
        String emisor,
        String mercado,
        UUID monedaId,
        String identificadorExterno,
        String descripcion,
        boolean activo,
        Instant fechaActualizacion
    ) {
        this.tipoInstrumentoId = tipoInstrumentoId;
        this.nombre = nombre;
        this.simbolo = simbolo;
        this.emisor = emisor;
        this.mercado = mercado;
        this.monedaId = monedaId;
        this.identificadorExterno = identificadorExterno;
        this.descripcion = descripcion;
        this.activo = activo;
        this.fechaActualizacion = fechaActualizacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTipoInstrumentoId() {
        return tipoInstrumentoId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public String getEmisor() {
        return emisor;
    }

    public String getMercado() {
        return mercado;
    }

    public UUID getMonedaId() {
        return monedaId;
    }

    public String getIdentificadorExterno() {
        return identificadorExterno;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
}
