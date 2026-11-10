package com.patrimonio.inversiones.adapter.out.persistence.adapter;

import com.patrimonio.inversiones.adapter.out.persistence.mapper.ClaseActivoMapper;
import com.patrimonio.inversiones.adapter.out.persistence.mapper.TipoInstrumentoMapper;
import com.patrimonio.inversiones.adapter.out.persistence.repository.ClaseActivoJpaRepository;
import com.patrimonio.inversiones.adapter.out.persistence.repository.TipoInstrumentoJpaRepository;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida del catálogo de clases y tipos de instrumento.
 */
@Component
public class CatalogoInstrumentoRepositoryAdapter
    implements CatalogoInstrumentoRepository {

    private final ClaseActivoJpaRepository clases;
    private final TipoInstrumentoJpaRepository tipos;
    private final ClaseActivoMapper claseMapper;
    private final TipoInstrumentoMapper tipoMapper;

    public CatalogoInstrumentoRepositoryAdapter(
        ClaseActivoJpaRepository clases,
        TipoInstrumentoJpaRepository tipos,
        ClaseActivoMapper claseMapper,
        TipoInstrumentoMapper tipoMapper
    ) {
        this.clases = clases;
        this.tipos = tipos;
        this.claseMapper = claseMapper;
        this.tipoMapper = tipoMapper;
    }

    @Override
    public List<ClaseActivo> listarClasesActivas() {
        return clases.findByActivoTrueOrderByNombreAsc().stream()
            .map(claseMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<ClaseActivo> buscarClasePorId(UUID claseActivoId) {
        return clases.findById(claseActivoId).map(claseMapper::toDomain);
    }

    @Override
    public List<ClaseActivo> buscarClasesPorIds(
        Collection<UUID> claseActivoIds
    ) {
        if (claseActivoIds.isEmpty()) {
            return List.of();
        }
        return clases.findAllById(claseActivoIds).stream()
            .map(claseMapper::toDomain)
            .toList();
    }

    @Override
    public List<TipoInstrumento> listarTiposActivos() {
        return tipos.findByActivoTrueOrderByNombreAsc().stream()
            .map(tipoMapper::toDomain)
            .toList();
    }

    @Override
    public List<TipoInstrumento> listarTiposActivosPorClase(
        UUID claseActivoId
    ) {
        return tipos.findByActivoTrueAndClaseActivoIdOrderByNombreAsc(
                claseActivoId)
            .stream()
            .map(tipoMapper::toDomain)
            .toList();
    }

    @Override
    public List<TipoInstrumento> buscarTiposPorIds(
        Collection<UUID> tipoInstrumentoIds
    ) {
        if (tipoInstrumentoIds.isEmpty()) {
            return List.of();
        }
        return tipos.findAllById(tipoInstrumentoIds).stream()
            .map(tipoMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<TipoInstrumento> buscarTipoActivoPorId(
        UUID tipoInstrumentoId
    ) {
        return tipos.findByIdAndActivoTrue(tipoInstrumentoId)
            .map(tipoMapper::toDomain);
    }

    @Override
    public Optional<TipoInstrumento> buscarTipoPorId(UUID tipoInstrumentoId) {
        return tipos.findById(tipoInstrumentoId).map(tipoMapper::toDomain);
    }
}
