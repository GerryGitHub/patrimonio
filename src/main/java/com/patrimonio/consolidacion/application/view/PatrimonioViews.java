package com.patrimonio.consolidacion.application.view;

import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import com.patrimonio.consolidacion.domain.model.GranularidadEvolucion;
import com.patrimonio.consolidacion.domain.model.PeriodoPatrimonial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Vistas de aplicación del Centro de Patrimonio.
 *
 * <p>Los importes viajan como {@link BigDecimal} dentro de la aplicación y el
 * adaptador HTTP los serializa como cadena con escala fija. Nunca se suman
 * monedas distintas: cada importe conserva su moneda y los porcentajes se
 * calculan dentro de una misma moneda.</p>
 */
public final class PatrimonioViews {

    private PatrimonioViews() {
    }

    public record MonedaView(
        UUID id,
        String codigo,
        String nombre,
        String simbolo,
        int decimales
    ) {
    }

    public record ImporteMonedaView(
        UUID monedaId,
        String codigo,
        String nombre,
        String simbolo,
        int decimales,
        BigDecimal importe
    ) {
        public static ImporteMonedaView desde(MonedaView moneda, BigDecimal importe) {
            return new ImporteMonedaView(
                moneda.id(),
                moneda.codigo(),
                moneda.nombre(),
                moneda.simbolo(),
                moneda.decimales(),
                importe);
        }
    }

    public record IndicadorView(
        String codigo,
        String etiqueta,
        EstadoInformacion estado,
        String motivo,
        List<ImporteMonedaView> importes
    ) {
    }

    public record PortafolioView(
        UUID id,
        String nombre,
        String monedaBaseCodigo,
        int monedaBaseDecimales
    ) {
    }

    public record CoberturaView(
        List<String> incluye,
        List<String> excluye
    ) {
    }

    public record MetaView(
        List<PortafolioView> portafolios,
        String monedaFiltro,
        PeriodoPatrimonial periodo,
        Instant generadoEn,
        EstadoInformacion estado,
        CoberturaView cobertura,
        List<String> limitaciones
    ) {
    }

    public record ResumenView(
        IndicadorView patrimonioNeto,
        IndicadorView activosRegistrados,
        IndicadorView pasivos,
        IndicadorView capitalInvertido,
        IndicadorView capitalAportado,
        IndicadorView liquidezDisponible,
        IndicadorView resultadoRealizado,
        IndicadorView resultadoNoRealizado
    ) {
    }

    public record SegmentoDistribucionView(
        String clave,
        String nombre,
        BigDecimal importe,
        BigDecimal porcentaje,
        int posiciones,
        int instrumentos
    ) {
    }

    public record DistribucionMonedaView(
        MonedaView moneda,
        BigDecimal total,
        List<SegmentoDistribucionView> segmentos
    ) {
    }

    public record DistribucionView(
        DimensionDistribucion dimension,
        List<DistribucionMonedaView> porMoneda,
        List<String> limitaciones
    ) {
    }

    public record PuntoEvolucionView(
        LocalDate fecha,
        BigDecimal valor
    ) {
    }

    public record SerieMonedaView(
        MonedaView moneda,
        List<PuntoEvolucionView> puntos
    ) {
    }

    public record SerieEvolucionView(
        String codigo,
        String etiqueta,
        EstadoInformacion estado,
        String motivo,
        List<SerieMonedaView> porMoneda
    ) {
    }

    public record DesgloseClaseView(
        UUID claseActivoId,
        String claseActivoNombre,
        List<SerieMonedaView> porMoneda
    ) {
    }

    public record EvolucionView(
        PeriodoPatrimonial periodo,
        LocalDate desde,
        LocalDate hasta,
        GranularidadEvolucion granularidad,
        List<SerieEvolucionView> series,
        List<DesgloseClaseView> desglosePorClase,
        List<String> limitaciones
    ) {
    }

    public record RendimientosView(
        LocalDate desde,
        LocalDate hasta,
        IndicadorView resultadoRealizado,
        IndicadorView comisionesPagadas,
        IndicadorView impuestosPagados,
        IndicadorView interesesRecibidos,
        IndicadorView dividendosRecibidos,
        IndicadorView resultadoNoRealizado,
        IndicadorView rendimientoPorcentual,
        List<String> metodologiasPrevistas,
        String nota
    ) {
    }

    public record CuentaLiquidezView(
        UUID cuentaId,
        String nombre,
        String institucionNombre,
        MonedaView moneda,
        boolean activa,
        BigDecimal saldo
    ) {
    }

    public record EfectivoCuentaView(
        EstadoInformacion estado,
        String motivo,
        List<CuentaLiquidezView> cuentas
    ) {
    }

    public record EfectivoInstitucionView(
        EstadoInformacion estado,
        String motivo,
        List<SegmentoDistribucionView> segmentos
    ) {
    }

    public record EventoView(
        UUID id,
        String tipo,
        LocalDate fecha,
        String descripcion,
        BigDecimal importe,
        MonedaView moneda,
        String origen,
        UUID posicionId,
        UUID contratoId
    ) {
    }

    public record EventosView(
        EstadoInformacion estado,
        String motivo,
        List<EventoView> eventos
    ) {
    }

    public record LiquidezView(
        EstadoInformacion estado,
        EfectivoCuentaView efectivoPorCuenta,
        EfectivoInstitucionView efectivoPorInstitucion,
        IndicadorView disponibilidadInmediata,
        IndicadorView capitalComprometido,
        EventosView proximosVencimientos
    ) {
    }

    public record CentroView(
        MetaView meta,
        ResumenView resumen,
        DistribucionView distribucion,
        EvolucionView evolucion,
        RendimientosView rendimientos,
        LiquidezView liquidez,
        EventosView eventos
    ) {
    }

    public record SnapshotView(
        UUID id,
        UUID portafolioId,
        LocalDate fecha,
        MonedaView moneda,
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
    }
}
