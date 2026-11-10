package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.IdentificadorInstrumentoEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data de identificadores de instrumento.
 */
public interface IdentificadorInstrumentoJpaRepository
    extends JpaRepository<IdentificadorInstrumentoEntity, UUID> {

    Optional<IdentificadorInstrumentoEntity> findByInstrumentoIdAndTipo(
        UUID instrumentoId,
        String tipo);

    List<IdentificadorInstrumentoEntity>
        findByInstrumentoIdInAndTipoAndActivoTrue(
            Collection<UUID> instrumentoIds,
            String tipo);

    @Query("""
        select count(x) > 0
        from IdentificadorInstrumentoEntity x
        where x.tipo = :tipo
          and x.valor = :valor
          and x.activo = true
          and (:excluirInstrumentoId is null or x.instrumentoId <> :excluirInstrumentoId)
        """)
    boolean existeActivo(
        @Param("tipo") String tipo,
        @Param("valor") String valor,
        @Param("excluirInstrumentoId") UUID excluirInstrumentoId);
}
