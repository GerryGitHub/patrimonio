package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.TipoInstrumentoEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data del catálogo de tipos de instrumento.
 */
public interface TipoInstrumentoJpaRepository
    extends JpaRepository<TipoInstrumentoEntity, UUID> {

    List<TipoInstrumentoEntity> findByActivoTrueOrderByNombreAsc();

    List<TipoInstrumentoEntity>
        findByActivoTrueAndClaseActivoIdOrderByNombreAsc(UUID claseActivoId);

    Optional<TipoInstrumentoEntity> findByIdAndActivoTrue(UUID id);
}
