package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.application.view.PatrimonioViews.ImporteMonedaView;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Acumulador de importes separados por moneda.
 *
 * <p>Cada moneda mantiene su propio total. La clase no convierte ni suma
 * monedas distintas; solo ordena las monedas por código al presentarlas.</p>
 */
public final class TotalesMoneda {

    private final Map<UUID, Total> totales = new LinkedHashMap<>();

    public void sumar(UUID monedaId, MonedaView moneda, BigDecimal importe) {
        totales.computeIfAbsent(monedaId, id -> new Total(moneda))
            .sumar(importe);
    }

    public boolean vacio() {
        return totales.isEmpty();
    }

    public List<ImporteMonedaView> importes() {
        return totales.values().stream()
            .sorted(Comparator.comparing(total -> total.moneda.codigo()))
            .map(total -> ImporteMonedaView.desde(
                total.moneda, total.importe))
            .toList();
    }

    private static final class Total {

        private final MonedaView moneda;
        private BigDecimal importe = BigDecimal.ZERO;

        private Total(MonedaView moneda) {
            this.moneda = moneda;
        }

        private void sumar(BigDecimal valor) {
            importe = importe.add(valor);
        }
    }
}
