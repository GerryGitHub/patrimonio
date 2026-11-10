package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.port.in.ListarPosicionesUseCase;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.query.FiltroInstrumentos;
import com.patrimonio.inversiones.application.query.ListarPosicionesQuery;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del listado de posiciones de un portafolio.
 *
 * <p>Primero se autoriza el portafolio y se obtienen sus cuentas. Las
 * posiciones se limitan a esas cuentas, de modo que un identificador de cuenta
 * de otro portafolio no amplía el resultado. El filtro por clase de activo se
 * resuelve en la aplicación porque la posición solo guarda el instrumento.</p>
 */
@Service
public class ListarPosicionesService implements ListarPosicionesUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final PosicionRepository posiciones;
    private final InversionesVistas vistas;

    public ListarPosicionesService(
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
    public List<PosicionView> execute(ListarPosicionesQuery query) {
        Objects.requireNonNull(query, "query");

        accesoPortafolio.exigirLectura(query.usuarioId(), query.portafolioId());

        List<CuentaResumen> cuentasDelPortafolio =
            cuentas.listarPorPortafolio(query.portafolioId());
        if (cuentasDelPortafolio.isEmpty()) {
            return List.of();
        }

        Map<UUID, CuentaResumen> cuentasPorId = cuentasDelPortafolio.stream()
            .collect(Collectors.toMap(CuentaResumen::id, Function.identity()));

        if (query.cuentaId() != null && !cuentasPorId.containsKey(query.cuentaId())) {
            return List.of();
        }

        Set<UUID> cuentasAutorizadas = cuentasPorId.keySet();

        Collection<UUID> instrumentosRestringidos = null;
        if (query.claseActivoId() != null) {
            List<Instrumento> deLaClase = instrumentos.listar(
                new FiltroInstrumentos(
                    null, query.claseActivoId(), null, null));
            if (deLaClase.isEmpty()) {
                return List.of();
            }
            instrumentosRestringidos = deLaClase.stream()
                .map(Instrumento::id)
                .collect(Collectors.toSet());
        }

        List<Posicion> encontradas = posiciones.listarPorCuentas(
            cuentasAutorizadas,
            query.cuentaId(),
            query.instrumentoId(),
            instrumentosRestringidos,
            query.activo());

        if (encontradas.isEmpty()) {
            return List.of();
        }

        List<Instrumento> referenciados = instrumentos.buscarPorIds(
            encontradas.stream()
                .map(Posicion::instrumentoId)
                .collect(Collectors.toSet()));

        return vistas.posiciones(encontradas, referenciados, cuentasPorId);
    }
}
