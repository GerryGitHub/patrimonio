package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.ClaseActivoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data del catálogo de clases de activo.
 */
public interface ClaseActivoJpaRepository
    extends JpaRepository<ClaseActivoEntity, UUID> {

    List<ClaseActivoEntity> findByActivoTrueOrderByNombreAsc();
}
