package com.patrimonio.cuentas.adapter.out.persistence.repository;

import com.patrimonio.cuentas.adapter.out.persistence.entity.InstitucionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA de instituciones financieras.
 */
public interface InstitucionJpaRepository
    extends JpaRepository<InstitucionEntity, UUID> {

    @Query("""
        select i
        from InstitucionEntity i
        where (:tipo is null or i.tipo = :tipo)
          and (:activo is null or i.activo = :activo)
        order by i.nombre asc, i.id asc
        """)
    List<InstitucionEntity> listar(
        @Param("tipo") String tipo,
        @Param("activo") Boolean activo);

    boolean existsByNombreIgnoreCase(String nombre);
}
