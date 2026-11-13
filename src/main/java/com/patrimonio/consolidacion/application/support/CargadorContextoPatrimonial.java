package com.patrimonio.consolidacion.application.support;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.cuentas.api.port.CuentaPatrimonioPort;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.inversiones.api.port.MovimientoPatrimonioPort;
import com.patrimonio.inversiones.api.port.PosicionPatrimonioPort;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Carga el contexto patrimonial compartido por todos los casos de uso.
 *
 * <p>Resuelve los portafolios autorizados, verifica el acceso cuando se
 * consulta un portafolio concreto y consulta las cuentas, posiciones y
 * movimientos a través de los puertos públicos de {@code acceso},
 * {@code cuentas} e {@code inversiones}. No accede a repositorios ni entidades
 * JPA de otros módulos.</p>
 */
@Component
public class CargadorContextoPatrimonial {

    private final AccesoPatrimonio acceso;
    private final PortafolioQueryPort portafolios;
    private final CuentaPatrimonioPort cuentas;
    private final PosicionPatrimonioPort posiciones;
    private final MovimientoPatrimonioPort movimientos;

    public CargadorContextoPatrimonial(
        AccesoPatrimonio acceso,
        PortafolioQueryPort portafolios,
        CuentaPatrimonioPort cuentas,
        PosicionPatrimonioPort posiciones,
        MovimientoPatrimonioPort movimientos
    ) {
        this.acceso = acceso;
        this.portafolios = portafolios;
        this.cuentas = cuentas;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
    }

    public ContextoPatrimonial cargar(ConsultarPatrimonioQuery query) {
        List<PortafolioResumen> autorizados = portafoliosDe(query);
        LocalDate hoy = LocalDate.now(ZoneOffset.UTC);
        List<PortafolioPatrimonio> datos = new ArrayList<>();
        for (PortafolioResumen portafolio : autorizados) {
            List<CuentaPatrimonio> cuentasPortafolio =
                cuentas.listarPorPortafolio(portafolio.id());
            Set<UUID> cuentaIds = cuentasPortafolio.stream()
                .map(CuentaPatrimonio::id)
                .collect(Collectors.toCollection(LinkedHashSet::new));
            List<PosicionPatrimonio> posicionesPortafolio = cuentaIds.isEmpty()
                ? List.of()
                : posiciones.listarPorCuentas(cuentaIds, true);
            List<MovimientoPatrimonio> movimientosPortafolio =
                movimientos.listarPorPortafolio(portafolio.id(), null, hoy);
            datos.add(new PortafolioPatrimonio(
                portafolio,
                cuentasPortafolio,
                posicionesPortafolio,
                movimientosPortafolio));
        }
        return new ContextoPatrimonial(datos, hoy);
    }

    private List<PortafolioResumen> portafoliosDe(
        ConsultarPatrimonioQuery query
    ) {
        if (query.portafolioId() == null) {
            return acceso.listarAutorizados(query.usuarioId());
        }
        acceso.exigirLectura(query.usuarioId(), query.portafolioId());
        PortafolioResumen portafolio = portafolios
            .buscarPorId(query.portafolioId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el portafolio indicado."));
        return List.of(portafolio);
    }
}
