package com.patrimonio.inversiones.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code posicion}.
 *
 * <p>{@code precio_promedio} es una vista del costo unitario.
 * {@code costo_total} es la magnitud autoritativa del costo de adquisición y
 * se actualiza transaccionalmente con los movimientos. La columna
 * {@code version} habilita el bloqueo optimista de JPA.</p>
 */
@Entity
@Table(name = "posicion", schema = "public")
public class PosicionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(name = "instrumento_id", nullable = false, updatable = false)
    private UUID instrumentoId;

    @Column(name = "contrato_id")
    private UUID contratoId;

    @Column(name = "cantidad", nullable = false, precision = 28, scale = 10)
    private BigDecimal cantidad;

    @Column(name = "precio_promedio", nullable = false, precision = 24, scale = 8)
    private BigDecimal precioPromedio;

    @Column(name = "costo_total", nullable = false, precision = 28, scale = 10)
    private BigDecimal costoTotal;

    @Column(name = "resultado_realizado", nullable = false, precision = 28, scale = 10)
    private BigDecimal resultadoRealizado;

    @Column(name = "fecha_ultimo_movimiento")
    private Instant fechaUltimoMovimiento;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "moneda_id", nullable = false)
    private UUID monedaId;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_apertura", nullable = false)
    private Instant fechaApertura;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    protected PosicionEntity() {
        // Constructor requerido por JPA.
    }

    public PosicionEntity(
        UUID id,
        UUID cuentaId,
        UUID instrumentoId,
        UUID contratoId,
        BigDecimal cantidad,
        BigDecimal precioPromedio,
        BigDecimal costoTotal,
        BigDecimal resultadoRealizado,
        Instant fechaUltimoMovimiento,
        UUID monedaId,
        String observaciones,
        boolean activo,
        Instant fechaApertura,
        Instant fechaCreacion,
        Instant fechaActualizacion
    ) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.instrumentoId = instrumentoId;
        this.contratoId = contratoId;
        this.cantidad = cantidad;
        this.precioPromedio = precioPromedio;
        this.costoTotal = costoTotal;
        this.resultadoRealizado = resultadoRealizado;
        this.fechaUltimoMovimiento = fechaUltimoMovimiento;
        this.monedaId = monedaId;
        this.observaciones = observaciones;
        this.activo = activo;
        this.fechaApertura = fechaApertura;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public void actualizar(
        BigDecimal cantidad,
        BigDecimal precioPromedio,
        BigDecimal costoTotal,
        BigDecimal resultadoRealizado,
        String observaciones,
        boolean activo,
        Instant fechaApertura,
        Instant fechaUltimoMovimiento,
        Instant fechaActualizacion
    ) {
        this.cantidad = cantidad;
        this.precioPromedio = precioPromedio;
        this.costoTotal = costoTotal;
        this.resultadoRealizado = resultadoRealizado;
        this.observaciones = observaciones;
        this.activo = activo;
        this.fechaApertura = fechaApertura;
        this.fechaUltimoMovimiento = fechaUltimoMovimiento;
        this.fechaActualizacion = fechaActualizacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public UUID getInstrumentoId() {
        return instrumentoId;
    }

    public UUID getContratoId() {
        return contratoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioPromedio() {
        return precioPromedio;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public BigDecimal getResultadoRealizado() {
        return resultadoRealizado;
    }

    public Instant getFechaUltimoMovimiento() {
        return fechaUltimoMovimiento;
    }

    public long getVersion() {
        return version;
    }

    public UUID getMonedaId() {
        return monedaId;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public boolean isActivo() {
        return activo;
    }

    public Instant getFechaApertura() {
        return fechaApertura;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public Instant getFechaActualizacion() {
        return fechaActualizacion;
    }
}
