package com.patrimonio.acceso.adapter.out.persistence.repository;

import com.patrimonio.acceso.adapter.out.persistence.entity.PortafolioEntity;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de solo lectura de portafolios.
 *
 * <p>La consulta proyecta directamente el DTO público para no cargar columnas
 * que no se necesitan.</p>
 */
public interface PortafolioJpaRepository extends JpaRepository<PortafolioEntity, UUID> {

    @Query("""
        select new com.patrimonio.acceso.api.dto.PortafolioResumen(
            p.id, p.nombre, p.monedaBaseId, p.activo
        )
        from PortafolioEntity p
        where p.id = :portafolioId
        """)
    Optional<PortafolioResumen> buscarResumenPorId(@Param("portafolioId") UUID portafolioId);
}
