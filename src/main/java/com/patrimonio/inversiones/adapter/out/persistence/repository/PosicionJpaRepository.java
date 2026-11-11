package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.PosicionEntity;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data de posiciones.
 *
 * <p>El listado exige el conjunto de cuentas autorizadas y nunca cruza
 * portafolios por sí mismo. Hay una variante con restricción de instrumentos
 * para el filtro por clase de activo; se separan las consultas para no pasar
 * una colección nula a una cláusula {@code in}.</p>
 */
public interface PosicionJpaRepository
    extends JpaRepository<PosicionEntity, UUID> {

    @Query("""
        select p
        from PosicionEntity p
        where p.cuentaId in :cuentaIds
          and (:cuentaId is null or p.cuentaId = :cuentaId)
          and (:instrumentoId is null or p.instrumentoId = :instrumentoId)
          and (:activo is null or p.activo = :activo)
        order by p.fechaCreacion asc
        """)
    List<PosicionEntity> listarPorCuentas(
        @Param("cuentaIds") Collection<UUID> cuentaIds,
        @Param("cuentaId") UUID cuentaId,
        @Param("instrumentoId") UUID instrumentoId,
        @Param("activo") Boolean activo);

    @Query("""
        select p
        from PosicionEntity p
        where p.cuentaId in :cuentaIds
          and p.instrumentoId in :instrumentoIds
          and (:cuentaId is null or p.cuentaId = :cuentaId)
          and (:instrumentoId is null or p.instrumentoId = :instrumentoId)
          and (:activo is null or p.activo = :activo)
        order by p.fechaCreacion asc
        """)
    List<PosicionEntity> listarPorCuentasEInstrumentos(
        @Param("cuentaIds") Collection<UUID> cuentaIds,
        @Param("instrumentoIds") Collection<UUID> instrumentoIds,
        @Param("cuentaId") UUID cuentaId,
        @Param("instrumentoId") UUID instrumentoId,
        @Param("activo") Boolean activo);

    /**
     * Bloqueo pesimista para serializar movimientos concurrentes de una misma
     * posición. Se mantiene hasta el commit de la transacción.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PosicionEntity p where p.id = :posicionId")
    Optional<PosicionEntity> buscarPorIdParaActualizar(
        @Param("posicionId") UUID posicionId);
}
