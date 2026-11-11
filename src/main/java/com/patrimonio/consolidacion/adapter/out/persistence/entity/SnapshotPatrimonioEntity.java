package com.patrimonio.consolidacion.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla aditiva {@code snapshot_patrimonio}.
 *
 * <p>Los campos de efectivo, pasivos, valor de mercado y patrimonio neto son
 * opcionales: permanecen {@code NULL} cuando no existe información confiable.
 * No se rellenan con el costo de adquisición.</p>
 */
@Entity
@Table(name = "snapshot_patrimonio", schema = "public")
public class SnapshotPatrimonioEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "portafolio_id", nullable = false, updatable = false)
    private UUID portafolioId;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(name = "moneda_id", nullable = false, updatable = false)
    private UUID monedaId;

    @Column(name = "capital_invertido", nullable = false, precision = 28,
        scale = 10, updatable = false)
    private BigDecimal capitalInvertido;

    @Column(name = "resultado_realizado", nullable = false, precision = 28,
        scale = 10, updatable = false)
    private BigDecimal resultadoRealizado;

    @Column(name = "efectivo", precision = 28, scale = 10, updatable = false)
    private BigDecimal efectivo;

    @Column(name = "pasivos", precision = 28, scale = 10, updatable = false)
    private BigDecimal pasivos;

    @Column(name = "valor_mercado", precision = 28, scale = 10,
        updatable = false)
    private BigDecimal valorMercado;

    @Column(name = "patrimonio_neto", precision = 28, scale = 10,
        updatable = false)
    private BigDecimal patrimonioNeto;

    @Column(name = "cobertura", nullable = false, length = 500,
        updatable = false)
    private String cobertura;

    @Column(name = "limitaciones", length = 1000, updatable = false)
    private String limitaciones;

    @Column(name = "fuente", nullable = false, length = 50, updatable = false)
    private String fuente;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected SnapshotPatrimonioEntity() {
        // Constructor requerido por JPA.
    }

    public SnapshotPatrimonioEntity(
        UUID id,
        UUID portafolioId,
        LocalDate fecha,
        UUID monedaId,
        BigDecimal capitalInvertido,
        BigDecimal resultadoRealizado,
        BigDecimal efectivo,
        BigDecimal pasivos,
        BigDecimal valorMercado,
        BigDecimal patrimonioNeto,
        String cobertura,
        String limitaciones,
        String fuente,
        Instant fechaCreacion
    ) {
        this.id = id;
        this.portafolioId = portafolioId;
        this.fecha = fecha;
        this.monedaId = monedaId;
        this.capitalInvertido = capitalInvertido;
        this.resultadoRealizado = resultadoRealizado;
        this.efectivo = efectivo;
        this.pasivos = pasivos;
        this.valorMercado = valorMercado;
        this.patrimonioNeto = patrimonioNeto;
        this.cobertura = cobertura;
        this.limitaciones = limitaciones;
        this.fuente = fuente;
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPortafolioId() {
        return portafolioId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public UUID getMonedaId() {
        return monedaId;
    }

    public BigDecimal getCapitalInvertido() {
        return capitalInvertido;
    }

    public BigDecimal getResultadoRealizado() {
        return resultadoRealizado;
    }

    public BigDecimal getEfectivo() {
        return efectivo;
    }

    public BigDecimal getPasivos() {
        return pasivos;
    }

    public BigDecimal getValorMercado() {
        return valorMercado;
    }

    public BigDecimal getPatrimonioNeto() {
        return patrimonioNeto;
    }

    public String getCobertura() {
        return cobertura;
    }

    public String getLimitaciones() {
        return limitaciones;
    }

    public String getFuente() {
        return fuente;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}
