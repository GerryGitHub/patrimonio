package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.MovimientoEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data de movimientos.
 */
public interface MovimientoJpaRepository
    extends JpaRepository<MovimientoEntity, UUID> {

    Optional<MovimientoEntity> findByPortafolioIdAndClaveIdempotencia(
        UUID portafolioId, String claveIdempotencia);

    Optional<MovimientoEntity> findByMovimientoOrigenId(UUID movimientoOrigenId);

    List<MovimientoEntity> findByMovimientoOrigenIdIn(
        Collection<UUID> movimientoOrigenIds);

    boolean existsByPosicionId(UUID posicionId);

    List<MovimientoEntity>
        findByPosicionIdOrderByFechaOperacionAscFechaCreacionAsc(
            UUID posicionId);

    @Query("""
        select m
        from MovimientoEntity m
        where m.posicionId = :posicionId
          and m.fechaOperacion >= :desde
          and m.fechaOperacion <= :hasta
        """)
    Page<MovimientoEntity> buscarPorRango(
        @Param("posicionId") UUID posicionId,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta,
        Pageable pageable);

    @Query("""
        select m
        from MovimientoEntity m
        where m.posicionId = :posicionId
          and m.tipoMovimientoId = :tipoId
          and m.fechaOperacion >= :desde
          and m.fechaOperacion <= :hasta
        """)
    Page<MovimientoEntity> buscarPorTipoYRango(
        @Param("posicionId") UUID posicionId,
        @Param("tipoId") UUID tipoMovimientoId,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta,
        Pageable pageable);
}
