package com.patrimonio.consolidacion.application.support;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.view.PatrimonioViews.MonedaView;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolución y comparación de monedas para las consultas patrimoniales.
 *
 * <p>Los importes conservan siempre su moneda. El filtro por código solo
 * selecciona qué monedas se presentan; nunca convierte importes.</p>
 */
@Component
public class MonedasPatrimonio {

    private final MonedaCatalogoPort monedas;

    public MonedasPatrimonio(MonedaCatalogoPort monedas) {
        this.monedas = monedas;
    }

    public MonedaView deCuenta(UUID monedaId) {
        MonedaResumen moneda = monedas.buscarPorId(monedaId)
            .orElseThrow(() -> new DatosPersistidosInvalidos(
                "La cuenta referencia una moneda inexistente: " + monedaId));
        return new MonedaView(
            moneda.id(),
            moneda.codigo(),
            moneda.nombre(),
            moneda.simbolo(),
            moneda.decimales());
    }

    public MonedaView dePosicion(PosicionPatrimonio posicion) {
        return new MonedaView(
            posicion.monedaId(),
            posicion.monedaCodigo(),
            posicion.monedaNombre(),
            posicion.monedaSimbolo(),
            posicion.monedaDecimales());
    }

    public MonedaView deMovimiento(MovimientoPatrimonio movimiento) {
        return new MonedaView(
            movimiento.monedaId(),
            movimiento.monedaCodigo(),
            movimiento.monedaNombre(),
            movimiento.monedaSimbolo(),
            movimiento.monedaDecimales());
    }

    public boolean coincide(
        ConsultarPatrimonioQuery query,
        String codigo
    ) {
        return coincide(query.monedaCodigo(), codigo);
    }

    public boolean coincide(String filtro, String codigo) {
        return filtro == null
            || (codigo != null && filtro.equalsIgnoreCase(codigo));
    }
}
