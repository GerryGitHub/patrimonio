package com.patrimonio.cuentas.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mapeo JPA de la tabla {@code cuenta_financiera}.
 *
 * <p>Entidad anémica: solo datos. No tiene asociaciones JPA; las referencias a
 * portafolio, institución, tipo de cuenta y moneda se mantienen como UUID, tal
 * como están en el esquema. No se usa {@code @GeneratedValue}: el ID lo asigna
 * la aplicación.</p>
 */
@Entity
@Table(name = "cuenta_financiera", schema = "public")
public class CuentaFinancieraEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "portafolio_id", nullable = false, updatable = false)
    private UUID portafolioId;

    @Column(name = "institucion_id", nullable = false, updatable = false)
    private UUID institucionId;

    @Column(name = "tipo_cuenta_id", nullable = false, updatable = false)
    private UUID tipoCuentaId;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "numero_cuenta", length = 64)
    private String numeroCuenta;

    @Column(name = "moneda_id", nullable = false, updatable = false)
    private UUID monedaId;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "fecha_apertura")
    private LocalDate fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    protected CuentaFinancieraEntity() {
        // Constructor requerido por JPA.
    }

    public CuentaFinancieraEntity(
        UUID id,
        UUID portafolioId,
        UUID institucionId,
        UUID tipoCuentaId,
        String nombre,
        String numeroCuenta,
        UUID monedaId,
        boolean activo,
        LocalDate fechaApertura,
        LocalDate fechaCierre
    ) {
        this.id = id;
        this.portafolioId = portafolioId;
        this.institucionId = institucionId;
        this.tipoCuentaId = tipoCuentaId;
        this.nombre = nombre;
        this.numeroCuenta = numeroCuenta;
        this.monedaId = monedaId;
        this.activo = activo;
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPortafolioId() {
        return portafolioId;
    }

    public UUID getInstitucionId() {
        return institucionId;
    }

    public UUID getTipoCuentaId() {
        return tipoCuentaId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public UUID getMonedaId() {
        return monedaId;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof CuentaFinancieraEntity cuenta
            && id != null
            && id.equals(cuenta.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
