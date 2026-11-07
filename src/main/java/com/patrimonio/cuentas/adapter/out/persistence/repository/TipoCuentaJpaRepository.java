package com.patrimonio.cuentas.adapter.out.persistence.repository;

import com.patrimonio.cuentas.adapter.out.persistence.entity.TipoCuentaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de solo lectura del catálogo de tipos de cuenta.
 */
public interface TipoCuentaJpaRepository
    extends JpaRepository<TipoCuentaEntity, UUID> {

    List<TipoCuentaEntity> findByActivoTrueOrderByNombreAsc();

    Optional<TipoCuentaEntity> findByIdAndActivoTrue(UUID id);
}
