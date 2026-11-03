package com.patrimonio.acceso.adapter.out.persistence.repository;

import com.patrimonio.acceso.adapter.out.persistence.entity.MembresiaEntity;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de solo lectura de membresías.
 *
 * <p>Las consultas exigen que el portafolio y el usuario estén activos. Si
 * falta cualquiera de las condiciones no se devuelve permiso, de modo que el
 * módulo consumidor responda igual que ante un portafolio inexistente.</p>
 */
public interface MembresiaJpaRepository
    extends JpaRepository<MembresiaEntity, UUID> {

    @Query("""
        select p.id
        from MembresiaEntity m, PortafolioEntity p, UsuarioEntity u
        where m.usuarioId = :usuarioId
          and m.portafolioId = :portafolioId
          and p.id = m.portafolioId
          and p.activo = true
          and u.id = m.usuarioId
          and u.activo = true
        """)
    Optional<UUID> buscarPortafolioAutorizado(
        @Param("usuarioId") UUID usuarioId,
        @Param("portafolioId") UUID portafolioId);

    @Query("""
        select new com.patrimonio.acceso.api.dto.PortafolioResumen(
            p.id, p.nombre, p.monedaBaseId, p.activo
        )
        from MembresiaEntity m, PortafolioEntity p, UsuarioEntity u
        where m.usuarioId = :usuarioId
          and p.id = m.portafolioId
          and p.activo = true
          and u.id = m.usuarioId
          and u.activo = true
        order by p.nombre, p.id
        """)
    List<PortafolioResumen> listarPortafoliosAutorizados(
        @Param("usuarioId") UUID usuarioId);
}
