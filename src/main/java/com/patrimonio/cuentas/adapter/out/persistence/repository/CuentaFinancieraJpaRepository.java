package com.patrimonio.cuentas.adapter.out.persistence.repository;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA de cuentas financieras.
 *
 * <p>El método de listado es explícito y filtra por portafolio; no se expone
 * {@code findAll()} sin filtro. El orden es determinista para que la respuesta
 * sea estable.</p>
 */
public interface CuentaFinancieraJpaRepository
    extends JpaRepository<CuentaFinancieraEntity, UUID> {

    @Query("""
        select c
        from CuentaFinancieraEntity c
        where c.portafolioId = :portafolioId
        order by c.nombre asc, c.id asc
        """)
    List<CuentaFinancieraEntity> listarPorPortafolio(
        @Param("portafolioId") UUID portafolioId);
}
