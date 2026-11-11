package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de cálculo de movimientos.
 *
 * <p>Selecciona la estrategia por tipo. El motor es puro: no conoce JPA, Spring
 * ni HTTP, y puede probarse con pruebas unitarias sin base de datos.</p>
 */
public final class MotorMovimientos {

    private final Map<TipoMovimiento, EstrategiaMovimiento> estrategias;

    public MotorMovimientos(Collection<EstrategiaMovimiento> estrategias) {
        this.estrategias = new EnumMap<>(TipoMovimiento.class);
        for (EstrategiaMovimiento estrategia : estrategias) {
            EstrategiaMovimiento anterior = this.estrategias.put(
                estrategia.tipo(), estrategia);
            if (anterior != null) {
                throw new IllegalArgumentException(
                    "Hay más de una estrategia para " + estrategia.tipo());
            }
        }
    }

    public static MotorMovimientos estandar() {
        return new MotorMovimientos(List.of(
            new EstrategiaApertura(),
            new EstrategiaCompra(),
            new EstrategiaVenta(),
            new EstrategiaAjustePositivo(),
            new EstrategiaAjusteNegativo(),
            new EstrategiaReversion()));
    }

    public ResultadoCalculo calcular(
        TipoMovimiento tipo,
        ContextoCalculo contexto
    ) {
        EstrategiaMovimiento estrategia = estrategias.get(tipo);
        if (estrategia == null) {
            throw new MovimientoInvalidoException(
                "No hay una estrategia para el tipo " + tipo + ".");
        }
        return estrategia.calcular(contexto);
    }
}
