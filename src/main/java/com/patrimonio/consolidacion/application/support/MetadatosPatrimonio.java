package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.CoberturaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MetaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.PortafolioView;
import com.patrimonio.consolidacion.domain.model.EstadoInformacion;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Metadatos, cobertura y limitaciones comunes del Centro de Patrimonio.
 */
@Component
public class MetadatosPatrimonio {

    private static final List<String> COBERTURA_INCLUYE = List.of(
        "INVERSIONES_A_COSTO",
        "RESULTADO_REALIZADO",
        "COMISIONES",
        "IMPUESTOS");

    private static final List<String> COBERTURA_EXCLUYE = List.of(
        "EFECTIVO",
        "ACTIVOS_FISICOS",
        "PASIVOS",
        "VALOR_MERCADO",
        "INTERESES",
        "DIVIDENDOS",
        "CAPITAL_APORTADO");

    private static final List<String> LIMITACIONES = List.of(
        "No hay valuación de mercado: el patrimonio neto y las ganancias no "
            + "realizadas no pueden calcularse.",
        "No hay saldos de efectivo registrados: la liquidez disponible no "
            + "puede calcularse.",
        "No hay pasivos registrados: el patrimonio neto no puede calcularse.",
        "No se modelan aportes ni retiros externos: el capital aportado "
            + "histórico no puede calcularse.",
        "El costo de adquisición no es valor de mercado ni efectivo "
            + "disponible.");

    public MetaView meta(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query
    ) {
        List<PortafolioView> vistas = contexto.portafolios().stream()
            .map(dato -> new PortafolioView(
                dato.portafolio().id(),
                dato.portafolio().nombre(),
                dato.portafolio().monedaBase().codigo(),
                dato.portafolio().monedaBase().decimales()))
            .toList();
        EstadoInformacion estado = vistas.isEmpty()
            ? EstadoInformacion.NO_DISPONIBLE
            : EstadoInformacion.PARCIAL;
        return new MetaView(
            vistas,
            query.monedaCodigo(),
            query.periodo(),
            Instant.now(),
            estado,
            new CoberturaView(COBERTURA_INCLUYE, COBERTURA_EXCLUYE),
            LIMITACIONES);
    }
}
