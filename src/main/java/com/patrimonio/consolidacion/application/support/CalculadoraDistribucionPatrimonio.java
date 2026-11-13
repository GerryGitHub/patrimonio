package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionMonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.DistribucionView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.SegmentoDistribucionView;
import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Cálculo de la distribución patrimonial por dimensión.
 *
 * <p>Los porcentajes se calculan dentro de cada moneda; nunca se suman
 * monedas distintas. La base es el costo de adquisición de las posiciones
 * activas, no el valor de mercado.</p>
 */
@Component
public class CalculadoraDistribucionPatrimonio {

    private static final int ESCALA_PORCENTAJE = 4;
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final MonedasPatrimonio monedas;

    public CalculadoraDistribucionPatrimonio(MonedasPatrimonio monedas) {
        this.monedas = monedas;
    }

    public DistribucionView calcular(
        ContextoPatrimonial contexto,
        ConsultarPatrimonioQuery query
    ) {
        Map<UUID, CuentaPatrimonio> cuentasPorId = cuentasPorId(contexto);
        Map<UUID, DistribucionMonedaAcumulada> porMoneda =
            new LinkedHashMap<>();

        for (PosicionPatrimonio posicion : contexto.posiciones()) {
            if (!monedas.coincide(query, posicion.monedaCodigo())) {
                continue;
            }
            ClaveSegmento clave = claveSegmento(
                query.dimension(), posicion, cuentasPorId);
            DistribucionMonedaAcumulada acumulada = porMoneda.computeIfAbsent(
                posicion.monedaId(),
                id -> new DistribucionMonedaAcumulada(
                    monedas.dePosicion(posicion)));
            acumulada.agregar(
                clave,
                posicion.costoTotal(),
                posicion.instrumentoId());
        }

        List<DistribucionMonedaView> vistas = porMoneda.values().stream()
            .map(acumulada -> acumulada.vista(query.dimension()))
            .sorted(Comparator.comparing(
                vista -> vista.moneda().codigo()))
            .toList();

        List<String> limitaciones = vistas.isEmpty()
            ? List.of("No hay posiciones activas registradas para distribuir.")
            : List.of(
                "Los porcentajes se calculan dentro de cada moneda; no se "
                    + "suman monedas distintas.",
                "La distribución usa el costo de adquisición de las "
                    + "posiciones activas, no valor de mercado.");

        return new DistribucionView(query.dimension(), vistas, limitaciones);
    }

    private ClaveSegmento claveSegmento(
        DimensionDistribucion dimension,
        PosicionPatrimonio posicion,
        Map<UUID, CuentaPatrimonio> cuentasPorId
    ) {
        return switch (dimension) {
            case CLASE_ACTIVO -> new ClaveSegmento(
                posicion.claseActivoId().toString(),
                posicion.claseActivoNombre());
            case TIPO_INSTRUMENTO -> new ClaveSegmento(
                posicion.tipoInstrumentoId().toString(),
                posicion.tipoInstrumentoNombre());
            case INSTITUCION -> {
                CuentaPatrimonio cuenta = cuentaRequerida(
                    cuentasPorId, posicion.cuentaId());
                yield new ClaveSegmento(
                    cuenta.institucionId().toString(),
                    cuenta.institucionNombre());
            }
            case CUENTA -> {
                CuentaPatrimonio cuenta = cuentaRequerida(
                    cuentasPorId, posicion.cuentaId());
                yield new ClaveSegmento(cuenta.id().toString(), cuenta.nombre());
            }
            case MONEDA -> new ClaveSegmento(
                posicion.monedaId().toString(),
                posicion.monedaCodigo());
        };
    }

    private static Map<UUID, CuentaPatrimonio> cuentasPorId(
        ContextoPatrimonial contexto
    ) {
        Map<UUID, CuentaPatrimonio> resultado = new HashMap<>();
        for (CuentaPatrimonio cuenta : contexto.cuentas()) {
            resultado.put(cuenta.id(), cuenta);
        }
        return resultado;
    }

    private static CuentaPatrimonio cuentaRequerida(
        Map<UUID, CuentaPatrimonio> cuentas,
        UUID cuentaId
    ) {
        CuentaPatrimonio cuenta = cuentas.get(cuentaId);
        if (cuenta == null) {
            throw new DatosPersistidosInvalidos(
                "La posición referencia una cuenta inexistente: " + cuentaId);
        }
        return cuenta;
    }

    private record ClaveSegmento(String clave, String nombre) {
    }

    private static final class DistribucionMonedaAcumulada {

        private final MonedaView moneda;
        private final Map<String, SegmentoAcumulado> segmentos =
            new LinkedHashMap<>();
        private BigDecimal total = BigDecimal.ZERO;

        private DistribucionMonedaAcumulada(MonedaView moneda) {
            this.moneda = moneda;
        }

        private void agregar(
            ClaveSegmento clave,
            BigDecimal importe,
            UUID instrumentoId
        ) {
            SegmentoAcumulado segmento = segmentos.computeIfAbsent(
                clave.clave(),
                id -> new SegmentoAcumulado(clave.clave(), clave.nombre()));
            segmento.importe = segmento.importe.add(importe);
            segmento.posiciones++;
            segmento.instrumentos.add(instrumentoId);
            total = total.add(importe);
        }

        private DistribucionMonedaView vista(DimensionDistribucion dimension) {
            List<SegmentoDistribucionView> vistas = segmentos.values()
                .stream()
                .map(segmento -> new SegmentoDistribucionView(
                    segmento.clave,
                    segmento.nombre,
                    segmento.importe,
                    dimension == DimensionDistribucion.MONEDA
                        ? null
                        : porcentaje(segmento.importe, total),
                    segmento.posiciones,
                    segmento.instrumentos.size()))
                .sorted(Comparator
                    .comparing(SegmentoDistribucionView::importe)
                    .reversed()
                    .thenComparing(SegmentoDistribucionView::nombre))
                .toList();
            return new DistribucionMonedaView(moneda, total, vistas);
        }

        private static BigDecimal porcentaje(
            BigDecimal importe,
            BigDecimal total
        ) {
            if (total.signum() == 0) {
                return null;
            }
            return importe.multiply(CIEN)
                .divide(total, ESCALA_PORCENTAJE, REDONDEO);
        }
    }

    private static final class SegmentoAcumulado {

        private final String clave;
        private final String nombre;
        private final Set<UUID> instrumentos = new HashSet<>();
        private BigDecimal importe = BigDecimal.ZERO;
        private int posiciones;

        private SegmentoAcumulado(String clave, String nombre) {
            this.clave = clave;
            this.nombre = nombre;
        }
    }
}
