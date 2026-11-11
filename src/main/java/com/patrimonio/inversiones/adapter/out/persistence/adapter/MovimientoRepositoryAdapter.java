package com.patrimonio.inversiones.adapter.out.persistence.adapter;

import com.patrimonio.inversiones.adapter.out.persistence.entity.MovimientoEntity;
import com.patrimonio.inversiones.adapter.out.persistence.entity.TipoMovimientoEntity;
import com.patrimonio.inversiones.adapter.out.persistence.mapper.MovimientoMapper;
import com.patrimonio.inversiones.adapter.out.persistence.repository.MovimientoJpaRepository;
import com.patrimonio.inversiones.adapter.out.persistence.repository.TipoMovimientoJpaRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
import com.patrimonio.inversiones.application.query.FiltroMovimientos;
import com.patrimonio.inversiones.application.query.PaginaMovimientos;
import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida de movimientos.
 */
@Component
public class MovimientoRepositoryAdapter implements MovimientoRepository {

    private static final LocalDate FECHA_MINIMA = LocalDate.of(1, 1, 1);
    private static final LocalDate FECHA_MAXIMA = LocalDate.of(9999, 12, 31);

    private final MovimientoJpaRepository movimientos;
    private final TipoMovimientoJpaRepository tipos;
    private final MovimientoMapper mapper;

    public MovimientoRepositoryAdapter(
        MovimientoJpaRepository movimientos,
        TipoMovimientoJpaRepository tipos,
        MovimientoMapper mapper
    ) {
        this.movimientos = movimientos;
        this.tipos = tipos;
        this.mapper = mapper;
    }

    @Override
    public Movimiento guardar(Movimiento movimiento) {
        MovimientoEntity guardado = movimientos.save(
            mapper.toEntity(movimiento, idDeTipo(movimiento.tipo())));
        return mapper.toDomain(guardado, mapaDeTipos());
    }

    @Override
    public Optional<Movimiento> buscarPorId(UUID movimientoId) {
        return movimientos.findById(movimientoId)
            .map(entity -> mapper.toDomain(entity, mapaDeTipos()));
    }

    @Override
    public Optional<Movimiento> buscarPorClaveIdempotencia(
        UUID portafolioId,
        String claveIdempotencia
    ) {
        return movimientos
            .findByPortafolioIdAndClaveIdempotencia(
                portafolioId, claveIdempotencia)
            .map(entity -> mapper.toDomain(entity, mapaDeTipos()));
    }

    @Override
    public Optional<Movimiento> buscarReversionDe(UUID movimientoOrigenId) {
        return movimientos.findByMovimientoOrigenId(movimientoOrigenId)
            .map(entity -> mapper.toDomain(entity, mapaDeTipos()));
    }

    @Override
    public Set<UUID> buscarOrigenesRevertidos(
        Collection<UUID> movimientoIds
    ) {
        if (movimientoIds.isEmpty()) {
            return Set.of();
        }
        return movimientos.findByMovimientoOrigenIdIn(movimientoIds).stream()
            .map(MovimientoEntity::getMovimientoOrigenId)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean existePorPosicion(UUID posicionId) {
        return movimientos.existsByPosicionId(posicionId);
    }

    @Override
    public List<Movimiento> listarPorPortafolio(
        UUID portafolioId,
        LocalDate desde,
        LocalDate hasta
    ) {
        LocalDate inicio = desde == null ? FECHA_MINIMA : desde;
        LocalDate fin = hasta == null ? FECHA_MAXIMA : hasta;
        Map<UUID, TipoMovimiento> mapa = mapaDeTipos();
        return movimientos
            .listarPorPortafolio(portafolioId, inicio, fin)
            .stream()
            .map(entity -> mapper.toDomain(entity, mapa))
            .toList();
    }

    @Override
    public List<Movimiento> listarPorPosicion(UUID posicionId) {
        Map<UUID, TipoMovimiento> mapa = mapaDeTipos();
        return movimientos
            .findByPosicionIdOrderByFechaOperacionAscFechaCreacionAsc(posicionId)
            .stream()
            .map(entity -> mapper.toDomain(entity, mapa))
            .toList();
    }

    @Override
    public PaginaMovimientos listarPorPosicion(
        UUID posicionId,
        FiltroMovimientos filtro,
        int pagina,
        int tamano,
        boolean descendente
    ) {
        Sort orden = descendente
            ? Sort.by(
                Sort.Order.desc("fechaOperacion"),
                Sort.Order.desc("fechaCreacion"))
            : Sort.by(
                Sort.Order.asc("fechaOperacion"),
                Sort.Order.asc("fechaCreacion"));
        // Se usan límites concretos en lugar de parámetros nulos: PostgreSQL no
        // puede inferir el tipo de un parámetro nulo en una comparación de
        // fechas.
        LocalDate desde = filtro.desde() == null
            ? FECHA_MINIMA
            : filtro.desde();
        LocalDate hasta = filtro.hasta() == null
            ? FECHA_MAXIMA
            : filtro.hasta();
        PageRequest paginacion = PageRequest.of(pagina, tamano, orden);
        Page<MovimientoEntity> resultado = filtro.tipo() == null
            ? movimientos.buscarPorRango(
                posicionId, desde, hasta, paginacion)
            : movimientos.buscarPorTipoYRango(
                posicionId, idDeTipo(filtro.tipo()), desde, hasta, paginacion);
        Map<UUID, TipoMovimiento> mapa = mapaDeTipos();
        return new PaginaMovimientos(
            resultado.getContent().stream()
                .map(entity -> mapper.toDomain(entity, mapa))
                .toList(),
            resultado.getTotalElements());
    }

    private UUID idDeTipo(TipoMovimiento tipo) {
        return tipos.findByNombre(tipo.codigo())
            .map(TipoMovimientoEntity::getId)
            .orElseThrow(() -> new DatosPersistidosInvalidos(
                "No existe el tipo de movimiento " + tipo.codigo()
                    + " en el catálogo."));
    }

    private Map<UUID, TipoMovimiento> mapaDeTipos() {
        Map<UUID, TipoMovimiento> resultado = new HashMap<>();
        for (TipoMovimientoEntity entity : tipos.findAll()) {
            try {
                resultado.put(
                    entity.getId(),
                    TipoMovimiento.desdeCodigo(entity.getNombre()));
            } catch (MovimientoInvalidoException ex) {
                // El catálogo puede contener tipos de otros módulos; solo se
                // traducen los que pertenecen al motor de posiciones.
            }
        }
        return resultado;
    }
}
