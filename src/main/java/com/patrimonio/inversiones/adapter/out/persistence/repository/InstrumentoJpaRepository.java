package com.patrimonio.inversiones.adapter.out.persistence.repository;

import com.patrimonio.inversiones.adapter.out.persistence.entity.InstrumentoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data del catálogo de instrumentos.
 *
 * <p>La búsqueda libre cubre nombre, símbolo, emisor, mercado, identificador
 * externo e ISIN. El ISIN se consulta con una subconsulta a
 * {@code identificador_instrumento}; no se duplica en la tabla de
 * instrumentos.</p>
 */
public interface InstrumentoJpaRepository
    extends JpaRepository<InstrumentoEntity, UUID> {

    @Query("""
        select i
        from InstrumentoEntity i
        where (
            coalesce(:busqueda, '') = ''
            or lower(i.nombre) like lower(concat('%', coalesce(:busqueda, ''), '%'))
            or lower(coalesce(i.simbolo, '')) like lower(concat('%', coalesce(:busqueda, ''), '%'))
            or lower(coalesce(i.emisor, '')) like lower(concat('%', coalesce(:busqueda, ''), '%'))
            or lower(coalesce(i.mercado, '')) like lower(concat('%', coalesce(:busqueda, ''), '%'))
            or lower(coalesce(i.identificadorExterno, '')) like lower(concat('%', coalesce(:busqueda, ''), '%'))
            or exists (
                select 1
                from IdentificadorInstrumentoEntity x
                where x.instrumentoId = i.id
                  and x.tipo = 'ISIN'
                  and x.activo = true
                  and lower(x.valor) like lower(concat('%', coalesce(:busqueda, ''), '%'))
            )
        )
          and (
            :claseActivoId is null
            or i.tipoInstrumentoId in (
                select t.id
                from TipoInstrumentoEntity t
                where t.claseActivoId = :claseActivoId
            )
          )
          and (:tipoInstrumentoId is null or i.tipoInstrumentoId = :tipoInstrumentoId)
          and (:activo is null or i.activo = :activo)
        order by i.nombre asc, i.id asc
        """)
    List<InstrumentoEntity> listar(
        @Param("busqueda") String busqueda,
        @Param("claseActivoId") UUID claseActivoId,
        @Param("tipoInstrumentoId") UUID tipoInstrumentoId,
        @Param("activo") Boolean activo);

    @Query("""
        select count(i) > 0
        from InstrumentoEntity i
        where i.simbolo = :simbolo
          and i.monedaId = :monedaId
          and (:excluirInstrumentoId is null or i.id <> :excluirInstrumentoId)
        """)
    boolean existeSimboloEnMoneda(
        @Param("simbolo") String simbolo,
        @Param("monedaId") UUID monedaId,
        @Param("excluirInstrumentoId") UUID excluirInstrumentoId);
}
