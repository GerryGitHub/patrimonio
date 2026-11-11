package com.patrimonio.inversiones.application.support;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.application.view.MovimientoView;
import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Resuelve las vistas de aplicación de instrumentos, posiciones y movimientos.
 *
 * <p>Agrupa las consultas a los catálogos para evitar una consulta por fila.
 * Si una referencia almacenada no existe, se falla con
 * {@link DatosPersistidosInvalidos} en lugar de devolver datos incompletos.</p>
 */
@Component
public class InversionesVistas {

    private final CatalogoInstrumentoRepository catalogo;
    private final MonedaCatalogoPort monedas;

    public InversionesVistas(
        CatalogoInstrumentoRepository catalogo,
        MonedaCatalogoPort monedas
    ) {
        this.catalogo = catalogo;
        this.monedas = monedas;
    }

    public InstrumentoView instrumento(Instrumento instrumento) {
        return instrumentos(List.of(instrumento))
            .get(instrumento.id());
    }

    public Map<UUID, InstrumentoView> instrumentos(
        Collection<Instrumento> instrumentos
    ) {
        if (instrumentos.isEmpty()) {
            return Map.of();
        }

        Map<UUID, TipoInstrumento> tipos = catalogo
            .buscarTiposPorIds(instrumentos.stream()
                .map(Instrumento::tipoInstrumentoId)
                .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(TipoInstrumento::id, Function.identity()));

        Map<UUID, ClaseActivo> clases = catalogo
            .buscarClasesPorIds(tipos.values().stream()
                .map(TipoInstrumento::claseActivoId)
                .collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(ClaseActivo::id, Function.identity()));

        Map<UUID, MonedaResumen> monedasPorId = new HashMap<>();
        for (Instrumento instrumento : instrumentos) {
            monedasPorId.computeIfAbsent(
                instrumento.monedaId(),
                id -> monedas.buscarPorId(id)
                    .orElseThrow(() -> new DatosPersistidosInvalidos(
                        "El instrumento " + instrumento.id()
                            + " referencia una moneda inexistente.")));
        }

        Map<UUID, InstrumentoView> resultado = new HashMap<>();
        for (Instrumento instrumento : instrumentos) {
            TipoInstrumento tipo = tipos.get(instrumento.tipoInstrumentoId());
            if (tipo == null) {
                throw new DatosPersistidosInvalidos(
                    "El instrumento " + instrumento.id()
                        + " referencia un tipo de instrumento inexistente.");
            }
            ClaseActivo clase = clases.get(tipo.claseActivoId());
            if (clase == null) {
                throw new DatosPersistidosInvalidos(
                    "El tipo de instrumento " + tipo.id()
                        + " referencia una clase de activo inexistente.");
            }
            MonedaResumen moneda = monedasPorId.get(instrumento.monedaId());
            resultado.put(instrumento.id(), new InstrumentoView(
                instrumento.id(),
                tipo.id(),
                tipo.nombre(),
                clase.id(),
                clase.nombre(),
                instrumento.nombre(),
                instrumento.simbolo().orElse(null),
                instrumento.emisor().orElse(null),
                instrumento.mercado().orElse(null),
                moneda.id(),
                moneda.codigo(),
                moneda.nombre(),
                moneda.simbolo(),
                moneda.decimales(),
                instrumento.identificadorExterno().orElse(null),
                instrumento.isin().orElse(null),
                instrumento.descripcion().orElse(null),
                instrumento.activo(),
                instrumento.fechaCreacion(),
                instrumento.fechaActualizacion()));
        }
        return resultado;
    }

    /**
     * Resuelve varias posiciones agrupando las consultas a los catálogos.
     *
     * @param posiciones posiciones a resolver
     * @param instrumentos instrumentos referenciados por las posiciones
     * @param cuentas cuentas referenciadas, indexadas por identificador
     */
    public List<PosicionView> posiciones(
        List<Posicion> posiciones,
        List<Instrumento> instrumentos,
        Map<UUID, CuentaResumen> cuentas
    ) {
        Map<UUID, InstrumentoView> vistasInstrumento = instrumentos(instrumentos);
        return posiciones.stream()
            .map(posicion -> {
                InstrumentoView instrumento = vistasInstrumento.get(
                    posicion.instrumentoId());
                if (instrumento == null) {
                    throw new DatosPersistidosInvalidos(
                        "La posición " + posicion.id()
                            + " referencia un instrumento inexistente.");
                }
                CuentaResumen cuenta = cuentas.get(posicion.cuentaId());
                if (cuenta == null) {
                    throw new DatosPersistidosInvalidos(
                        "La posición " + posicion.id()
                            + " referencia una cuenta inexistente.");
                }
                return posicion(posicion, instrumento, cuenta);
            })
            .toList();
    }

    public PosicionView posicion(
        Posicion posicion,
        InstrumentoView instrumento,
        CuentaResumen cuenta
    ) {
        return new PosicionView(
            posicion.id(),
            posicion.cuentaId(),
            cuenta.nombre(),
            instrumento.id(),
            instrumento.nombre(),
            instrumento.simbolo(),
            instrumento.tipoInstrumentoId(),
            instrumento.tipoInstrumentoNombre(),
            instrumento.claseActivoId(),
            instrumento.claseActivoNombre(),
            posicion.cantidad(),
            posicion.costoUnitario(),
            posicion.costoTotal(),
            posicion.resultadoRealizado(),
            instrumento.monedaId(),
            instrumento.monedaCodigo(),
            instrumento.monedaNombre(),
            instrumento.monedaSimbolo(),
            instrumento.monedaDecimales(),
            posicion.observaciones().orElse(null),
            posicion.activo(),
            posicion.fechaApertura().atZone(ZoneOffset.UTC).toLocalDate(),
            posicion.fechaUltimoMovimiento().orElse(null),
            posicion.fechaCreacion(),
            posicion.fechaActualizacion());
    }

    /**
     * Resuelve una lista de movimientos de una o varias posiciones.
     */
    public List<MovimientoView> movimientos(
        List<Movimiento> movimientos,
        List<Instrumento> instrumentos,
        Map<UUID, CuentaResumen> cuentas,
        Set<UUID> revertidos
    ) {
        Map<UUID, InstrumentoView> vistasInstrumento = instrumentos(instrumentos);
        return movimientos.stream()
            .map(movimiento -> {
                InstrumentoView instrumento = vistasInstrumento.get(
                    movimiento.instrumentoId());
                if (instrumento == null) {
                    throw new DatosPersistidosInvalidos(
                        "El movimiento " + movimiento.id()
                            + " referencia un instrumento inexistente.");
                }
                CuentaResumen cuenta = cuentas.get(movimiento.cuentaId());
                if (cuenta == null) {
                    throw new DatosPersistidosInvalidos(
                        "El movimiento " + movimiento.id()
                            + " referencia una cuenta inexistente.");
                }
                return movimiento(
                    movimiento,
                    instrumento,
                    cuenta,
                    revertidos.contains(movimiento.id()));
            })
            .toList();
    }

    public MovimientoView movimiento(
        Movimiento movimiento,
        InstrumentoView instrumento,
        CuentaResumen cuenta,
        boolean revertido
    ) {
        return new MovimientoView(
            movimiento.id(),
            movimiento.portafolioId(),
            movimiento.cuentaId(),
            cuenta.nombre(),
            movimiento.posicionId(),
            movimiento.instrumentoId(),
            instrumento.nombre(),
            instrumento.simbolo(),
            movimiento.tipo(),
            movimiento.tipo().etiqueta(),
            movimiento.fechaOperacion(),
            movimiento.fechaLiquidacion().orElse(null),
            movimiento.cantidad(),
            movimiento.precioUnitario(),
            movimiento.importeBruto(),
            movimiento.comisiones(),
            movimiento.impuestos(),
            instrumento.monedaId(),
            instrumento.monedaCodigo(),
            instrumento.monedaNombre(),
            instrumento.monedaSimbolo(),
            instrumento.monedaDecimales(),
            movimiento.tipoCambio(),
            movimiento.importeNeto(),
            movimiento.costoAplicado(),
            movimiento.resultadoRealizado().orElse(null),
            movimiento.cantidadResultante(),
            movimiento.costoResultante(),
            movimiento.estado().name(),
            movimiento.usuarioId().orElse(null),
            movimiento.movimientoOrigenId().orElse(null),
            revertido,
            movimiento.observaciones().orElse(null),
            movimiento.fechaCreacion());
    }
}
