package com.patrimonio.cuentas.adapter.out.persistence.mapper;

import com.patrimonio.cuentas.adapter.out.persistence.entity.InstitucionEntity;
import com.patrimonio.cuentas.domain.model.Institucion;
import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre la entidad JPA y el agregado de dominio.
 */
@Component
public class InstitucionMapper {

    public Institucion toDomain(InstitucionEntity entity) {
        try {
            TipoInstitucion tipo = entity.getTipo() == null
                || entity.getTipo().isBlank()
                ? TipoInstitucion.OTRA
                : TipoInstitucion.desde(entity.getTipo());

            return Institucion.reconstruir(
                entity.getId(),
                entity.getNombre(),
                tipo,
                entity.getPais(),
                entity.getSitioWeb(),
                entity.isActivo());
        } catch (DomainException ex) {
            throw new DatosPersistidosInvalidos(
                "La institución almacenada no cumple las invariantes de dominio.",
                ex);
        }
    }

    public InstitucionEntity toEntity(Institucion institucion) {
        return new InstitucionEntity(
            institucion.id(),
            institucion.nombre(),
            institucion.tipo().name(),
            institucion.pais().orElse(null),
            institucion.sitioWeb().orElse(null),
            institucion.activo());
    }
}
