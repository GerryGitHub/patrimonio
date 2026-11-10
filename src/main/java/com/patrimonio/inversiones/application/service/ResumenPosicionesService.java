package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.port.in.ResumenPosicionesUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.query.ResumenPosicionesQuery;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.application.view.ResumenPosicionesView;
import com.patrimonio.inversiones.application.view.ResumenPosicionesView.CostoPorMonedaView;
import com.patrimonio.inversiones.application.view.ResumenPosicionesView.DistribucionCategoriaView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de los agregados de posiciones de un portafolio.
 *
 * <p>Solo se agregan las posiciones activas. El costo total es
 * {@code cantidad × costo unitario}; no es valor de mercado ni incluye
 * rendimiento. Los importes de monedas distintas no se suman entre sí: se
 * agrupan por moneda.</p>
 */
@Service
public class ResumenPosicionesService implements ResumenPosicionesUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final InversionesVistas vistas;

    public ResumenPosicionesService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        PosicionRepository posiciones,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.posiciones = posiciones;
        this.vistas = vistas;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenPosicionesView execute(ResumenPosicionesQuery query) {
        Objects.requireNonNull(query, "query");

        accesoPortafolio.exigirLectura(query.usuarioId(), query.portafolioId());

        List<CuentaResumen> cuentasDelPortafolio =
            cuentas.listarPorPortafolio(query.portafolioId());
        if (cuentasDelPortafolio.isEmpty()) {
            return vacio();
        }

        Set<UUID> cuentasAutorizadas = cuentasDelPortafolio.stream()
            .map(CuentaResumen::id)
            .collect(Collectors.toSet());

        List<Posicion> todas = posiciones.listarPorCuentas(
            cuentasAutorizadas, null, null, null, null);
        if (todas.isEmpty()) {
            return vacio();
        }

        List<Posicion> activas = todas.stream()
            .filter(Posicion::activo)
            .toList();

        if (activas.isEmpty()) {
            return new ResumenPosicionesView(
                todas.size(), 0, 0, List.of(), List.of());
        }

        List<Instrumento> referenciados = instrumentos.buscarPorIds(
            activas.stream()
                .map(Posicion::instrumentoId)
                .collect(Collectors.toSet()));
        Map<UUID, InstrumentoView> vistasInstrumento =
            vistas.instrumentos(referenciados);

        Set<UUID> instrumentosDistintos = new LinkedHashSet<>();
        Map<UUID, AcumuladoMoneda> porMoneda = new LinkedHashMap<>();
        Map<UUID, AcumuladoClase> porClase = new LinkedHashMap<>();

        for (Posicion posicion : activas) {
            InstrumentoView instrumento = vistasInstrumento.get(
                posicion.instrumentoId());
            if (instrumento == null) {
                throw new DatosPersistidosInvalidos(
                    "La posición " + posicion.id()
                        + " referencia un instrumento inexistente.");
            }

            instrumentosDistintos.add(instrumento.id());
            BigDecimal costo = posicion.costoTotal();

            porMoneda.computeIfAbsent(
                instrumento.monedaId(),
                id -> new AcumuladoMoneda(instrumento))
                .sumar(costo);

            porClase.computeIfAbsent(
                instrumento.claseActivoId(),
                id -> new AcumuladoClase(instrumento))
                .agregar(instrumento.id(), costo, instrumento);
        }

        List<CostoPorMonedaView> costoPorMoneda = porMoneda.values().stream()
            .map(AcumuladoMoneda::vista)
            .sorted(Comparator.comparing(CostoPorMonedaView::codigo))
            .toList();

        List<DistribucionCategoriaView> distribucion = porClase.values().stream()
            .map(AcumuladoClase::vista)
            .sorted(Comparator.comparing(DistribucionCategoriaView::claseActivoNombre))
            .toList();

        return new ResumenPosicionesView(
            todas.size(),
            activas.size(),
            instrumentosDistintos.size(),
            costoPorMoneda,
            distribucion);
    }

    private static ResumenPosicionesView vacio() {
        return new ResumenPosicionesView(0, 0, 0, List.of(), List.of());
    }

    private static final class AcumuladoMoneda {

        private final InstrumentoView instrumento;
        private BigDecimal costo = BigDecimal.ZERO;

        private AcumuladoMoneda(InstrumentoView instrumento) {
            this.instrumento = instrumento;
        }

        private void sumar(BigDecimal importe) {
            costo = costo.add(importe);
        }

        private CostoPorMonedaView vista() {
            return new CostoPorMonedaView(
                instrumento.monedaId(),
                instrumento.monedaCodigo(),
                instrumento.monedaNombre(),
                instrumento.monedaSimbolo(),
                instrumento.monedaDecimales(),
                costo);
        }
    }

    private static final class AcumuladoClase {

        private final UUID claseActivoId;
        private final String claseActivoNombre;
        private final Set<UUID> instrumentos = new LinkedHashSet<>();
        private final Map<UUID, AcumuladoMoneda> porMoneda = new LinkedHashMap<>();
        private int posiciones;

        private AcumuladoClase(InstrumentoView instrumento) {
            this.claseActivoId = instrumento.claseActivoId();
            this.claseActivoNombre = instrumento.claseActivoNombre();
        }

        private void agregar(
            UUID instrumentoId,
            BigDecimal costo,
            InstrumentoView instrumento
        ) {
            posiciones++;
            instrumentos.add(instrumentoId);
            porMoneda.computeIfAbsent(
                instrumento.monedaId(),
                id -> new AcumuladoMoneda(instrumento))
                .sumar(costo);
        }

        private DistribucionCategoriaView vista() {
            List<CostoPorMonedaView> costos = new ArrayList<>(
                porMoneda.values().stream()
                    .map(AcumuladoMoneda::vista)
                    .sorted(Comparator.comparing(CostoPorMonedaView::codigo))
                    .toList());
            return new DistribucionCategoriaView(
                claseActivoId,
                claseActivoNombre,
                posiciones,
                instrumentos.size(),
                costos);
        }
    }
}
