package com.patrimonio.consolidacion.domain.model;

import com.patrimonio.consolidacion.domain.error.SnapshotInvalidoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Fotografía manual del patrimonio de un portafolio en una moneda.
 *
 * <p>El capital invertido y el resultado realizado son datos reales derivados
 * de posiciones y movimientos. El efectivo, los pasivos, el valor de mercado y
 * el patrimonio neto permanecen {@code null} mientras no exista una fuente
 * confiable: nunca se rellenan con el costo de adquisición.</p>
 *
 * <p>La cobertura identifica qué parte del patrimonio representa el snapshot
 * para que no se presente como patrimonio neto completo.</p>
 */
public final class SnapshotPatrimonio {

    public static final String FUENTE_MANUAL = "MANUAL";
    public static final String COBERTURA_INVERSIONES = "INVERSIONES_A_COSTO";
    public static final String LIMITACIONES_POR_DEFECTO =
        "No incluye efectivo, pasivos ni valor de mercado.";

    private final UUID id;
    private final UUID portafolioId;
    private final LocalDate fecha;
    private final UUID monedaId;
    private final BigDecimal capitalInvertido;
    private final BigDecimal resultadoRealizado;
    private final BigDecimal efectivo;
    private final BigDecimal pasivos;
    private final BigDecimal valorMercado;
    private final BigDecimal patrimonioNeto;
    private final String cobertura;
    private final String limitaciones;
    private final String fuente;
    private final Instant fechaCreacion;

    private SnapshotPatrimonio(
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

    public static SnapshotPatrimonio crear(
        UUID id,
        UUID portafolioId,
        LocalDate fecha,
        UUID monedaId,
        BigDecimal capitalInvertido,
        BigDecimal resultadoRealizado,
        String cobertura,
        String limitaciones,
        Instant ahora
    ) {
        requerir(id, "id");
        requerir(portafolioId, "portafolioId");
        requerir(fecha, "fecha");
        requerir(monedaId, "monedaId");
        requerir(capitalInvertido, "capitalInvertido");
        requerir(resultadoRealizado, "resultadoRealizado");
        requerir(ahora, "ahora");
        if (capitalInvertido.signum() < 0) {
            throw new SnapshotInvalidoException(
                "El capital invertido no puede ser negativo.");
        }
        String coberturaFinal = textoOpcional(cobertura);
        if (coberturaFinal == null) {
            throw new SnapshotInvalidoException(
                "La cobertura del snapshot es obligatoria.");
        }
        return new SnapshotPatrimonio(
            id,
            portafolioId,
            fecha,
            monedaId,
            capitalInvertido,
            resultadoRealizado,
            null,
            null,
            null,
            null,
            coberturaFinal,
            textoOpcional(limitaciones),
            FUENTE_MANUAL,
            ahora);
    }

    public static SnapshotPatrimonio reconstruir(
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
        return new SnapshotPatrimonio(
            Objects.requireNonNull(id, "id"),
            Objects.requireNonNull(portafolioId, "portafolioId"),
            Objects.requireNonNull(fecha, "fecha"),
            Objects.requireNonNull(monedaId, "monedaId"),
            Objects.requireNonNull(capitalInvertido, "capitalInvertido"),
            Objects.requireNonNull(resultadoRealizado, "resultadoRealizado"),
            efectivo,
            pasivos,
            valorMercado,
            patrimonioNeto,
            cobertura,
            limitaciones,
            fuente,
            Objects.requireNonNull(fechaCreacion, "fechaCreacion"));
    }

    private static void requerir(Object valor, String campo) {
        if (valor == null) {
            throw new SnapshotInvalidoException(
                "El campo '" + campo + "' es obligatorio.");
        }
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    public UUID id() {
        return id;
    }

    public UUID portafolioId() {
        return portafolioId;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public UUID monedaId() {
        return monedaId;
    }

    public BigDecimal capitalInvertido() {
        return capitalInvertido;
    }

    public BigDecimal resultadoRealizado() {
        return resultadoRealizado;
    }

    public BigDecimal efectivo() {
        return efectivo;
    }

    public BigDecimal pasivos() {
        return pasivos;
    }

    public BigDecimal valorMercado() {
        return valorMercado;
    }

    public BigDecimal patrimonioNeto() {
        return patrimonioNeto;
    }

    public String cobertura() {
        return cobertura;
    }

    public String limitaciones() {
        return limitaciones;
    }

    public String fuente() {
        return fuente;
    }

    public Instant fechaCreacion() {
        return fechaCreacion;
    }
}
