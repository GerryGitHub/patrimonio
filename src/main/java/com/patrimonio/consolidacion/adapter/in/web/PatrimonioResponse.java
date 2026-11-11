package com.patrimonio.consolidacion.adapter.in.web;

import java.util.List;
import java.util.UUID;

/**
 * DTOs de respuesta del Centro de Patrimonio.
 *
 * <p>Todos los importes monetarios y porcentajes viajan como cadena con escala
 * fija para evitar pérdida de precisión en JavaScript. Las fechas se serializan
 * en formato ISO. Los indicadores no disponibles incluyen un motivo y una lista
 * vacía; nunca se devuelve cero como si fuera un dato real.</p>
 */
public final class PatrimonioResponse {

    private PatrimonioResponse() {
    }

    public record MonedaResponse(
        UUID id,
        String codigo,
        String nombre,
        String simbolo,
        int decimales
    ) {
    }

    public record ImporteMonedaResponse(
        UUID monedaId,
        String codigo,
        String nombre,
        String simbolo,
        int decimales,
        String importe
    ) {
    }

    public record IndicadorResponse(
        String codigo,
        String etiqueta,
        String estado,
        String motivo,
        List<ImporteMonedaResponse> importes
    ) {
    }

    public record PortafolioResponse(
        UUID id,
        String nombre,
        String monedaBaseCodigo,
        int monedaBaseDecimales
    ) {
    }

    public record CoberturaResponse(
        List<String> incluye,
        List<String> excluye
    ) {
    }

    public record MetaResponse(
        List<PortafolioResponse> portafolios,
        String monedaFiltro,
        String periodo,
        String generadoEn,
        String estado,
        CoberturaResponse cobertura,
        List<String> limitaciones
    ) {
    }

    public record ResumenResponse(
        IndicadorResponse patrimonioNeto,
        IndicadorResponse activosRegistrados,
        IndicadorResponse pasivos,
        IndicadorResponse capitalInvertido,
        IndicadorResponse capitalAportado,
        IndicadorResponse liquidezDisponible,
        IndicadorResponse resultadoRealizado,
        IndicadorResponse resultadoNoRealizado
    ) {
    }

    public record SegmentoDistribucionResponse(
        String clave,
        String nombre,
        String importe,
        String porcentaje,
        int posiciones,
        int instrumentos
    ) {
    }

    public record DistribucionMonedaResponse(
        MonedaResponse moneda,
        String total,
        List<SegmentoDistribucionResponse> segmentos
    ) {
    }

    public record DistribucionResponse(
        String dimension,
        List<DistribucionMonedaResponse> porMoneda,
        List<String> limitaciones
    ) {
    }

    public record PuntoEvolucionResponse(
        String fecha,
        String valor
    ) {
    }

    public record SerieMonedaResponse(
        MonedaResponse moneda,
        List<PuntoEvolucionResponse> puntos
    ) {
    }

    public record SerieEvolucionResponse(
        String codigo,
        String etiqueta,
        String estado,
        String motivo,
        List<SerieMonedaResponse> porMoneda
    ) {
    }

    public record DesgloseClaseResponse(
        String claseActivoId,
        String claseActivoNombre,
        List<SerieMonedaResponse> porMoneda
    ) {
    }

    public record EvolucionResponse(
        String periodo,
        String desde,
        String hasta,
        String granularidad,
        List<SerieEvolucionResponse> series,
        List<DesgloseClaseResponse> desglosePorClase,
        List<String> limitaciones
    ) {
    }

    public record RendimientosResponse(
        String desde,
        String hasta,
        IndicadorResponse resultadoRealizado,
        IndicadorResponse comisionesPagadas,
        IndicadorResponse impuestosPagados,
        IndicadorResponse interesesRecibidos,
        IndicadorResponse dividendosRecibidos,
        IndicadorResponse resultadoNoRealizado,
        IndicadorResponse rendimientoPorcentual,
        List<String> metodologiasPrevistas,
        String nota
    ) {
    }

    public record CuentaLiquidezResponse(
        UUID cuentaId,
        String nombre,
        String institucionNombre,
        MonedaResponse moneda,
        boolean activa,
        String saldo
    ) {
    }

    public record EfectivoCuentaResponse(
        String estado,
        String motivo,
        List<CuentaLiquidezResponse> cuentas
    ) {
    }

    public record EfectivoInstitucionResponse(
        String estado,
        String motivo,
        List<SegmentoDistribucionResponse> segmentos
    ) {
    }

    public record EventoResponse(
        UUID id,
        String tipo,
        String fecha,
        String descripcion,
        String importe,
        MonedaResponse moneda,
        String origen,
        UUID posicionId,
        UUID contratoId
    ) {
    }

    public record EventosResponse(
        String estado,
        String motivo,
        List<EventoResponse> eventos
    ) {
    }

    public record LiquidezResponse(
        String estado,
        EfectivoCuentaResponse efectivoPorCuenta,
        EfectivoInstitucionResponse efectivoPorInstitucion,
        IndicadorResponse disponibilidadInmediata,
        IndicadorResponse capitalComprometido,
        EventosResponse proximosVencimientos
    ) {
    }

    public record CentroResponse(
        MetaResponse meta,
        ResumenResponse resumen,
        DistribucionResponse distribucion,
        EvolucionResponse evolucion,
        RendimientosResponse rendimientos,
        LiquidezResponse liquidez,
        EventosResponse eventos
    ) {
    }

    public record SnapshotResponse(
        UUID id,
        UUID portafolioId,
        String fecha,
        MonedaResponse moneda,
        String capitalInvertido,
        String resultadoRealizado,
        String efectivo,
        String pasivos,
        String valorMercado,
        String patrimonioNeto,
        String cobertura,
        String limitaciones,
        String fuente,
        String fechaCreacion
    ) {
    }
}
