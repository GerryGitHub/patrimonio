package com.patrimonio.inversiones.adapter.out.persistence.adapter;

import com.patrimonio.inversiones.adapter.out.persistence.entity.IdentificadorInstrumentoEntity;
import com.patrimonio.inversiones.adapter.out.persistence.entity.InstrumentoEntity;
import com.patrimonio.inversiones.adapter.out.persistence.mapper.InstrumentoMapper;
import com.patrimonio.inversiones.adapter.out.persistence.repository.IdentificadorInstrumentoJpaRepository;
import com.patrimonio.inversiones.adapter.out.persistence.repository.InstrumentoJpaRepository;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.query.FiltroInstrumentos;
import com.patrimonio.inversiones.domain.model.Instrumento;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida del catálogo de instrumentos.
 *
 * <p>El ISIN se guarda en {@code identificador_instrumento} con
 * {@code tipo = 'ISIN'}. Al editarlo se actualiza la misma fila; al quitarlo se
 * desactiva para liberar el índice único parcial sin borrar historial.</p>
 */
@Component
public class InstrumentoRepositoryAdapter implements InstrumentoRepository {

    private static final String TIPO_ISIN =
        IdentificadorInstrumentoEntity.TIPO_ISIN;

    private final InstrumentoJpaRepository instrumentos;
    private final IdentificadorInstrumentoJpaRepository identificadores;
    private final InstrumentoMapper mapper;

    public InstrumentoRepositoryAdapter(
        InstrumentoJpaRepository instrumentos,
        IdentificadorInstrumentoJpaRepository identificadores,
        InstrumentoMapper mapper
    ) {
        this.instrumentos = instrumentos;
        this.identificadores = identificadores;
        this.mapper = mapper;
    }

    @Override
    public List<Instrumento> listar(FiltroInstrumentos filtro) {
        FiltroInstrumentos aplicado = filtro == null
            ? new FiltroInstrumentos(null, null, null, null)
            : filtro;
        List<InstrumentoEntity> entidades = instrumentos.listar(
            aplicado.busqueda(),
            aplicado.claseActivoId(),
            aplicado.tipoInstrumentoId(),
            aplicado.activo());
        return aDominio(entidades);
    }

    @Override
    public Optional<Instrumento> buscarPorId(UUID instrumentoId) {
        return instrumentos.findById(instrumentoId).map(this::aDominio);
    }

    @Override
    public List<Instrumento> buscarPorIds(Collection<UUID> instrumentoIds) {
        if (instrumentoIds.isEmpty()) {
            return List.of();
        }
        return aDominio(instrumentos.findAllById(instrumentoIds));
    }

    @Override
    public boolean existeSimboloEnMoneda(
        String simbolo,
        UUID monedaId,
        UUID excluirInstrumentoId
    ) {
        return instrumentos.existeSimboloEnMoneda(
            simbolo, monedaId, excluirInstrumentoId);
    }

    @Override
    public boolean existeIsin(String isin, UUID excluirInstrumentoId) {
        return identificadores.existeActivo(TIPO_ISIN, isin, excluirInstrumentoId);
    }

    @Override
    public Instrumento guardar(Instrumento instrumento) {
        Optional<InstrumentoEntity> existente =
            instrumentos.findById(instrumento.id());

        InstrumentoEntity guardado;
        if (existente.isPresent()) {
            InstrumentoEntity entidad = existente.get();
            mapper.actualizarEntidad(entidad, instrumento);
            guardado = instrumentos.save(entidad);
        } else {
            guardado = instrumentos.save(mapper.toEntity(instrumento));
        }

        sincronizarIsin(instrumento);
        return mapper.toDomain(guardado, instrumento.isin().orElse(null));
    }

    private void sincronizarIsin(Instrumento instrumento) {
        Optional<IdentificadorInstrumentoEntity> existente =
            identificadores.findByInstrumentoIdAndTipo(
                instrumento.id(), TIPO_ISIN);

        String isin = instrumento.isin().orElse(null);
        if (isin == null) {
            existente.filter(IdentificadorInstrumentoEntity::isActivo)
                .ifPresent(actual -> {
                    actual.desactivar();
                    identificadores.saveAndFlush(actual);
                });
            return;
        }

        if (existente.isPresent()) {
            IdentificadorInstrumentoEntity actual = existente.get();
            if (!isin.equals(actual.getValor()) || !actual.isActivo()) {
                actual.actualizar(isin, true);
                identificadores.saveAndFlush(actual);
            }
            return;
        }

        identificadores.saveAndFlush(new IdentificadorInstrumentoEntity(
            UUID.randomUUID(),
            instrumento.id(),
            TIPO_ISIN,
            isin,
            null,
            true));
    }

    private List<Instrumento> aDominio(List<InstrumentoEntity> entidades) {
        if (entidades.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> isinPorInstrumento = isinPorInstrumento(
            entidades.stream().map(InstrumentoEntity::getId).toList());
        return entidades.stream()
            .map(entidad -> mapper.toDomain(
                entidad,
                isinPorInstrumento.get(entidad.getId())))
            .toList();
    }

    private Instrumento aDominio(InstrumentoEntity entidad) {
        return mapper.toDomain(entidad, isinDe(entidad.getId()));
    }

    private String isinDe(UUID instrumentoId) {
        return identificadores
            .findByInstrumentoIdAndTipo(instrumentoId, TIPO_ISIN)
            .filter(IdentificadorInstrumentoEntity::isActivo)
            .map(IdentificadorInstrumentoEntity::getValor)
            .orElse(null);
    }

    private Map<UUID, String> isinPorInstrumento(Collection<UUID> ids) {
        Map<UUID, String> resultado = new HashMap<>();
        identificadores
            .findByInstrumentoIdInAndTipoAndActivoTrue(ids, TIPO_ISIN)
            .forEach(identificador -> resultado.put(
                identificador.getInstrumentoId(),
                identificador.getValor()));
        return resultado;
    }
}
