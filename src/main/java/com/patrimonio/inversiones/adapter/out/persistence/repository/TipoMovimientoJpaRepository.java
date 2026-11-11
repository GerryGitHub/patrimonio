package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.TipoMovimientoEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del catálogo de tipos de movimiento.
 */
public interface TipoMovimientoJpaRepository
    extends JpaRepository<TipoMovimientoEntity, UUID> {

    Optional<TipoMovimientoEntity> findByNombre(String nombre);

    List<TipoMovimientoEntity> findByActivoTrue();
}
