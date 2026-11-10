package com.patrimonio.inversiones.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code identificador_instrumento}.
 *
 * <p>En esta entrega se usa para el ISIN, con {@code tipo = 'ISIN'}. El
 * identificador externo del proveedor se guarda en la columna homónima de
 * {@code instrumento}.</p>
 */
@Entity
@Table(name = "identificador_instrumento", schema = "public")
public class IdentificadorInstrumentoEntity {

    public static final String TIPO_ISIN = "ISIN";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "instrumento_id", nullable = false, updatable = false)
    private UUID instrumentoId;

    @Column(name = "tipo", nullable = false, length = 50, updatable = false)
    private String tipo;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "pais", length = 100)
    private String pais;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    protected IdentificadorInstrumentoEntity() {
        // Constructor requerido por JPA.
    }

    public IdentificadorInstrumentoEntity(
        UUID id,
        UUID instrumentoId,
        String tipo,
        String valor,
        String pais,
        boolean activo
    ) {
        this.id = id;
        this.instrumentoId = instrumentoId;
        this.tipo = tipo;
        this.valor = valor;
        this.pais = pais;
        this.activo = activo;
    }

    public void actualizar(String valor, boolean activo) {
        this.valor = valor;
        this.activo = activo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInstrumentoId() {
        return instrumentoId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public String getPais() {
        return pais;
    }

    public boolean isActivo() {
        return activo;
    }
}
