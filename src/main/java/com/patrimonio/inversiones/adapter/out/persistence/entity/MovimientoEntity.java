package com.patrimonio.inversiones.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code movimiento}.
 *
 * <p>La fila es inmutable una vez creada. Los importes derivados se guardan
 * como {@code NUMERIC} y se leen como {@link BigDecimal}.</p>
 */
@Entity
@Table(name = "movimiento", schema = "public")
public class MovimientoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "portafolio_id", nullable = false, updatable = false)
    private UUID portafolioId;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(name = "posicion_id", nullable = false, updatable = false)
    private UUID posicionId;

    @Column(name = "instrumento_id", nullable = false, updatable = false)
    private UUID instrumentoId;

    @Column(name = "tipo_movimiento_id", nullable = false, updatable = false)
    private UUID tipoMovimientoId;

    @Column(name = "fecha_operacion", nullable = false, updatable = false)
    private LocalDate fechaOperacion;

    @Column(name = "fecha_liquidacion", updatable = false)
    private LocalDate fechaLiquidacion;

    @Column(name = "cantidad", nullable = false, precision = 28, scale = 10,
        updatable = false)
    private BigDecimal cantidad;

    @Column(name = "precio", nullable = false, precision = 24, scale = 8,
        updatable = false)
    private BigDecimal precio;

    @Column(name = "importe", nullable = false, precision = 28, scale = 10,
        updatable = false)
    private BigDecimal importe;

    @Column(name = "moneda_id", nullable = false, updatable = false)
    private UUID monedaId;

    @Column(name = "comision", nullable = false, precision = 20, scale = 10,
        updatable = false)
    private BigDecimal comision;

    @Column(name = "impuestos", nullable = false, precision = 20, scale = 10,
        updatable = false)
    private BigDecimal impuestos;

    @Column(name = "tipo_cambio", nullable = false, precision = 20, scale = 10,
        updatable = false)
    private BigDecimal tipoCambio;

    @Column(name = "importe_neto", nullable = false, precision = 28, scale = 10,
        updatable = false)
    private BigDecimal importeNeto;

    @Column(name = "costo_aplicado", nullable = false, precision = 28, scale = 10,
        updatable = false)
    private BigDecimal costoAplicado;

    @Column(name = "resultado_realizado", precision = 28, scale = 10,
        updatable = false)
    private BigDecimal resultadoRealizado;

    @Column(name = "efecto_cantidad", nullable = false, precision = 28, scale = 10,
        updatable = false)
    private BigDecimal efectoCantidad;

    @Column(name = "efecto_costo", nullable = false, precision = 28, scale = 10,
        updatable = false)
    private BigDecimal efectoCosto;

    @Column(name = "cantidad_resultante", nullable = false, precision = 28,
        scale = 10, updatable = false)
    private BigDecimal cantidadResultante;

    @Column(name = "costo_resultante", nullable = false, precision = 28,
        scale = 10, updatable = false)
    private BigDecimal costoResultante;

    @Column(name = "estado", nullable = false, length = 30, updatable = false)
    private String estado;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "clave_idempotencia", length = 100, updatable = false)
    private String claveIdempotencia;

    @Column(name = "movimiento_origen_id", updatable = false)
    private UUID movimientoOrigenId;

    @Column(name = "descripcion", length = 500, updatable = false)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected MovimientoEntity() {
        // Constructor requerido por JPA.
    }

    public MovimientoEntity(
        UUID id,
        UUID portafolioId,
        UUID cuentaId,
        UUID posicionId,
        UUID instrumentoId,
        UUID tipoMovimientoId,
        LocalDate fechaOperacion,
        LocalDate fechaLiquidacion,
        BigDecimal cantidad,
        BigDecimal precio,
        BigDecimal importe,
        UUID monedaId,
        BigDecimal comision,
        BigDecimal impuestos,
        BigDecimal tipoCambio,
        BigDecimal importeNeto,
        BigDecimal costoAplicado,
        BigDecimal resultadoRealizado,
        BigDecimal efectoCantidad,
        BigDecimal efectoCosto,
        BigDecimal cantidadResultante,
        BigDecimal costoResultante,
        String estado,
        UUID usuarioId,
        String claveIdempotencia,
        UUID movimientoOrigenId,
        String descripcion,
        Instant fechaCreacion
    ) {
        this.id = id;
        this.portafolioId = portafolioId;
        this.cuentaId = cuentaId;
        this.posicionId = posicionId;
        this.instrumentoId = instrumentoId;
        this.tipoMovimientoId = tipoMovimientoId;
        this.fechaOperacion = fechaOperacion;
        this.fechaLiquidacion = fechaLiquidacion;
        this.cantidad = cantidad;
        this.precio = precio;
        this.importe = importe;
        this.monedaId = monedaId;
        this.comision = comision;
        this.impuestos = impuestos;
        this.tipoCambio = tipoCambio;
        this.importeNeto = importeNeto;
        this.costoAplicado = costoAplicado;
        this.resultadoRealizado = resultadoRealizado;
        this.efectoCantidad = efectoCantidad;
        this.efectoCosto = efectoCosto;
        this.cantidadResultante = cantidadResultante;
        this.costoResultante = costoResultante;
        this.estado = estado;
        this.usuarioId = usuarioId;
        this.claveIdempotencia = claveIdempotencia;
        this.movimientoOrigenId = movimientoOrigenId;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPortafolioId() {
        return portafolioId;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public UUID getPosicionId() {
        return posicionId;
    }

    public UUID getInstrumentoId() {
        return instrumentoId;
    }

    public UUID getTipoMovimientoId() {
        return tipoMovimientoId;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public LocalDate getFechaLiquidacion() {
        return fechaLiquidacion;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public BigDecimal getImporte() {
        return importe;
    }

    public UUID getMonedaId() {
        return monedaId;
    }

    public BigDecimal getComision() {
        return comision;
    }

    public BigDecimal getImpuestos() {
        return impuestos;
    }

    public BigDecimal getTipoCambio() {
        return tipoCambio;
    }

    public BigDecimal getImporteNeto() {
        return importeNeto;
    }

    public BigDecimal getCostoAplicado() {
        return costoAplicado;
    }

    public BigDecimal getResultadoRealizado() {
        return resultadoRealizado;
    }

    public BigDecimal getEfectoCantidad() {
        return efectoCantidad;
    }

    public BigDecimal getEfectoCosto() {
        return efectoCosto;
    }

    public BigDecimal getCantidadResultante() {
        return cantidadResultante;
    }

    public BigDecimal getCostoResultante() {
        return costoResultante;
    }

    public String getEstado() {
        return estado;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public UUID getMovimientoOrigenId() {
        return movimientoOrigenId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}
