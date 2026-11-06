package com.patrimonio.acceso.adapter.out.persistence.repository;

import com.patrimonio.acceso.adapter.out.persistence.entity.MonedaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de solo lectura del catálogo de monedas.
 */
public interface MonedaJpaRepository
    extends JpaRepository<MonedaEntity, UUID> {

    List<MonedaEntity> findByActivoTrueOrderByCodigoAsc();

    Optional<MonedaEntity> findByIdAndActivoTrue(UUID id);
}
