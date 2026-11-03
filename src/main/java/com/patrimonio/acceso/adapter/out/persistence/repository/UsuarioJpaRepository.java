package com.patrimonio.acceso.adapter.out.persistence.repository;

import com.patrimonio.acceso.adapter.out.persistence.entity.UsuarioEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de usuarios para autenticación y registro.
 */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    @Query("""
        select u
        from UsuarioEntity u
        where lower(u.email) = :email
        """)
    Optional<UsuarioEntity> buscarPorEmail(@Param("email") String email);

    boolean existsByEmailIgnoreCase(String email);
}
