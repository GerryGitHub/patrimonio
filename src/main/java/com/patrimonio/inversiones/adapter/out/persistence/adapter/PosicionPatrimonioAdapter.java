package com.patrimonio.inversiones.adapter.out.persistence.adapter;

import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import com.patrimonio.inversiones.api.port.PosicionPatrimonioPort;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adaptador del contrato público de posiciones patrimoniales.
 *
 * <p>Resuelve los catálogos de instrumento, clase, tipo y moneda en lote para
 * no ejecutar una consulta por posición. No modifica el estado materializado ni
 * el motor de movimientos.</p>
 */
@Component
public class PosicionPatrimonioAdapter implements PosicionPatrimonioPort {

    private final PosicionRepository posiciones;
    private final InstrumentoRepository instrumentos;
    private final InversionesVistas vistas;

    public PosicionPatrimonioAdapter(
        PosicionRepository posiciones,
        InstrumentoRepository instrumentos,
        InversionesVistas vistas
    ) {
        this.posiciones = posiciones;
        this.instrumentos = instrumentos;
        this.vistas = vistas;
    }

    @Override
    public List<PosicionPatrimonio> listarPorCuentas(
        Collection<UUID> cuentaIds,
        boolean soloActivas
    ) {
        if (cuentaIds.isEmpty()) {
            return List.of();
        }
        List<Posicion> encontradas = posiciones.listarPorCuentas(
            cuentaIds, null, null, null, soloActivas ? Boolean.TRUE : null);
        if (encontradas.isEmpty()) {
            return List.of();
        }

        Set<UUID> instrumentoIds = encontradas.stream()
            .map(Posicion::instrumentoId)
            .collect(Collectors.toSet());
        List<Instrumento> referenciados =
            instrumentos.buscarPorIds(instrumentoIds);
        Map<UUID, InstrumentoView> vistasInstrumento =
            vistas.instrumentos(referenciados);

        return encontradas.stream()
            .map(posicion -> convertir(posicion, vistasInstrumento))
            .toList();
    }

    private PosicionPatrimonio convertir(
        Posicion posicion,
        Map<UUID, InstrumentoView> vistasInstrumento
    ) {
        InstrumentoView instrumento = vistasInstrumento.get(
            posicion.instrumentoId());
        if (instrumento == null) {
            throw new DatosPersistidosInvalidos(
                "La posición " + posicion.id()
                    + " referencia un instrumento inexistente.");
        }
        return new PosicionPatrimonio(
            posicion.id(),
            posicion.cuentaId(),
            instrumento.id(),
            instrumento.nombre(),
            instrumento.simbolo(),
            instrumento.claseActivoId(),
            instrumento.claseActivoNombre(),
            instrumento.tipoInstrumentoId(),
            instrumento.tipoInstrumentoNombre(),
            posicion.cantidad(),
            posicion.costoTotal(),
            posicion.resultadoRealizado(),
            instrumento.monedaId(),
            instrumento.monedaCodigo(),
            instrumento.monedaNombre(),
            instrumento.monedaSimbolo(),
            instrumento.monedaDecimales(),
            posicion.activo(),
            posicion.fechaApertura().atZone(ZoneOffset.UTC).toLocalDate(),
            posicion.fechaUltimoMovimiento().orElse(null));
    }
}
