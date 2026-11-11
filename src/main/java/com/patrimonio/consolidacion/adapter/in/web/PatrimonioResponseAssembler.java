package com.patrimonio.consolidacion.adapter.in.web;

import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.CentroResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.CoberturaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.CuentaLiquidezResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.DesgloseClaseResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.DistribucionMonedaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.DistribucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EfectivoCuentaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EfectivoInstitucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EventoResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EventosResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EvolucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.ImporteMonedaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.IndicadorResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.LiquidezResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.MetaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.MonedaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.PortafolioResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.PuntoEvolucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.RendimientosResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.ResumenResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.SegmentoDistribucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.SerieEvolucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.SerieMonedaResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.SnapshotResponse;
import com.patrimonio.consolidacion.application.view.PatrimonioViews;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Ensambla las vistas de aplicación con los DTOs HTTP.
 *
 * <p>Convierte todos los importes monetarios y porcentajes a cadena con escala
 * fija. Los campos no disponibles se mantienen como {@code null}; no se
 * reemplazan por cero.</p>
 */
@Component
public class PatrimonioResponseAssembler {

    private static final int ESCALA_IMPORTE = 10;
    private static final int ESCALA_PORCENTAJE = 4;

    public CentroResponse centro(PatrimonioViews.CentroView vista) {
        return new CentroResponse(
            meta(vista.meta()),
            resumen(vista.resumen()),
            distribucion(vista.distribucion()),
            evolucion(vista.evolucion()),
            rendimientos(vista.rendimientos()),
            liquidez(vista.liquidez()),
            eventos(vista.eventos()));
    }

    public ResumenResponse resumen(PatrimonioViews.ResumenView vista) {
        return new ResumenResponse(
            indicador(vista.patrimonioNeto()),
            indicador(vista.activosRegistrados()),
            indicador(vista.pasivos()),
            indicador(vista.capitalInvertido()),
            indicador(vista.capitalAportado()),
            indicador(vista.liquidezDisponible()),
            indicador(vista.resultadoRealizado()),
            indicador(vista.resultadoNoRealizado()));
    }

    public DistribucionResponse distribucion(
        PatrimonioViews.DistribucionView vista
    ) {
        return new DistribucionResponse(
            vista.dimension().name(),
            vista.porMoneda().stream()
                .map(this::distribucionMoneda)
                .toList(),
            vista.limitaciones());
    }

    public EvolucionResponse evolucion(PatrimonioViews.EvolucionView vista) {
        return new EvolucionResponse(
            vista.periodo().codigo(),
            fecha(vista.desde()),
            fecha(vista.hasta()),
            vista.granularidad().name(),
            vista.series().stream().map(this::serie).toList(),
            vista.desglosePorClase().stream().map(this::desglose).toList(),
            vista.limitaciones());
    }

    public RendimientosResponse rendimientos(
        PatrimonioViews.RendimientosView vista
    ) {
        return new RendimientosResponse(
            fecha(vista.desde()),
            fecha(vista.hasta()),
            indicador(vista.resultadoRealizado()),
            indicador(vista.comisionesPagadas()),
            indicador(vista.impuestosPagados()),
            indicador(vista.interesesRecibidos()),
            indicador(vista.dividendosRecibidos()),
            indicador(vista.resultadoNoRealizado()),
            indicador(vista.rendimientoPorcentual()),
            vista.metodologiasPrevistas(),
            vista.nota());
    }

    public LiquidezResponse liquidez(PatrimonioViews.LiquidezView vista) {
        return new LiquidezResponse(
            vista.estado().name(),
            new EfectivoCuentaResponse(
                vista.efectivoPorCuenta().estado().name(),
                vista.efectivoPorCuenta().motivo(),
                vista.efectivoPorCuenta().cuentas().stream()
                    .map(this::cuentaLiquidez)
                    .toList()),
            new EfectivoInstitucionResponse(
                vista.efectivoPorInstitucion().estado().name(),
                vista.efectivoPorInstitucion().motivo(),
                vista.efectivoPorInstitucion().segmentos().stream()
                    .map(this::segmento)
                    .toList()),
            indicador(vista.disponibilidadInmediata()),
            indicador(vista.capitalComprometido()),
            eventos(vista.proximosVencimientos()));
    }

    public EventosResponse eventos(PatrimonioViews.EventosView vista) {
        return new EventosResponse(
            vista.estado().name(),
            vista.motivo(),
            vista.eventos().stream().map(this::evento).toList());
    }

    public SnapshotResponse snapshot(PatrimonioViews.SnapshotView vista) {
        return new SnapshotResponse(
            vista.id(),
            vista.portafolioId(),
            fecha(vista.fecha()),
            moneda(vista.moneda()),
            importe(vista.capitalInvertido()),
            importe(vista.resultadoRealizado()),
            importe(vista.efectivo()),
            importe(vista.pasivos()),
            importe(vista.valorMercado()),
            importe(vista.patrimonioNeto()),
            vista.cobertura(),
            vista.limitaciones(),
            vista.fuente(),
            instante(vista.fechaCreacion()));
    }

    private MetaResponse meta(PatrimonioViews.MetaView vista) {
        return new MetaResponse(
            vista.portafolios().stream().map(this::portafolio).toList(),
            vista.monedaFiltro(),
            vista.periodo().codigo(),
            instante(vista.generadoEn()),
            vista.estado().name(),
            new CoberturaResponse(
                vista.cobertura().incluye(),
                vista.cobertura().excluye()),
            vista.limitaciones());
    }

    private PortafolioResponse portafolio(PatrimonioViews.PortafolioView vista) {
        return new PortafolioResponse(
            vista.id(),
            vista.nombre(),
            vista.monedaBaseCodigo(),
            vista.monedaBaseDecimales());
    }

    private IndicadorResponse indicador(PatrimonioViews.IndicadorView vista) {
        return new IndicadorResponse(
            vista.codigo(),
            vista.etiqueta(),
            vista.estado().name(),
            vista.motivo(),
            vista.importes().stream().map(this::importe).toList());
    }

    private ImporteMonedaResponse importe(
        PatrimonioViews.ImporteMonedaView vista
    ) {
        return new ImporteMonedaResponse(
            vista.monedaId(),
            vista.codigo(),
            vista.nombre(),
            vista.simbolo(),
            vista.decimales(),
            importe(vista.importe()));
    }

    private DistribucionMonedaResponse distribucionMoneda(
        PatrimonioViews.DistribucionMonedaView vista
    ) {
        return new DistribucionMonedaResponse(
            moneda(vista.moneda()),
            importe(vista.total()),
            vista.segmentos().stream().map(this::segmento).toList());
    }

    private SegmentoDistribucionResponse segmento(
        PatrimonioViews.SegmentoDistribucionView vista
    ) {
        return new SegmentoDistribucionResponse(
            vista.clave(),
            vista.nombre(),
            importe(vista.importe()),
            porcentaje(vista.porcentaje()),
            vista.posiciones(),
            vista.instrumentos());
    }

    private SerieEvolucionResponse serie(
        PatrimonioViews.SerieEvolucionView vista
    ) {
        return new SerieEvolucionResponse(
            vista.codigo(),
            vista.etiqueta(),
            vista.estado().name(),
            vista.motivo(),
            vista.porMoneda().stream().map(this::serieMoneda).toList());
    }

    private SerieMonedaResponse serieMoneda(
        PatrimonioViews.SerieMonedaView vista
    ) {
        return new SerieMonedaResponse(
            moneda(vista.moneda()),
            vista.puntos().stream().map(this::punto).toList());
    }

    private PuntoEvolucionResponse punto(
        PatrimonioViews.PuntoEvolucionView vista
    ) {
        return new PuntoEvolucionResponse(
            fecha(vista.fecha()),
            importe(vista.valor()));
    }

    private DesgloseClaseResponse desglose(
        PatrimonioViews.DesgloseClaseView vista
    ) {
        return new DesgloseClaseResponse(
            vista.claseActivoId() == null ? null : vista.claseActivoId().toString(),
            vista.claseActivoNombre(),
            vista.porMoneda().stream().map(this::serieMoneda).toList());
    }

    private CuentaLiquidezResponse cuentaLiquidez(
        PatrimonioViews.CuentaLiquidezView vista
    ) {
        return new CuentaLiquidezResponse(
            vista.cuentaId(),
            vista.nombre(),
            vista.institucionNombre(),
            moneda(vista.moneda()),
            vista.activa(),
            importe(vista.saldo()));
    }

    private EventoResponse evento(PatrimonioViews.EventoView vista) {
        return new EventoResponse(
            vista.id(),
            vista.tipo(),
            fecha(vista.fecha()),
            vista.descripcion(),
            importe(vista.importe()),
            vista.moneda() == null ? null : moneda(vista.moneda()),
            vista.origen(),
            vista.posicionId(),
            vista.contratoId());
    }

    private MonedaResponse moneda(PatrimonioViews.MonedaView vista) {
        return new MonedaResponse(
            vista.id(),
            vista.codigo(),
            vista.nombre(),
            vista.simbolo(),
            vista.decimales());
    }

    private static String importe(BigDecimal valor) {
        return valor == null
            ? null
            : valor.setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private static String porcentaje(BigDecimal valor) {
        return valor == null
            ? null
            : valor.setScale(ESCALA_PORCENTAJE, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private static String fecha(LocalDate valor) {
        return valor == null ? null : valor.toString();
    }

    private static String instante(Instant valor) {
        return valor == null ? null : valor.toString();
    }
}
