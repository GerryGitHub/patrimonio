package com.patrimonio.cuentas.adapter.out.persistence.mapper;

import com.patrimonio.cuentas.adapter.out.persistence.entity.TipoCuentaEntity;
import com.patrimonio.cuentas.domain.model.TipoCuenta;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre la entidad JPA y el tipo de cuenta de dominio.
 */
@Component
public class TipoCuentaMapper {

    public TipoCuenta toDomain(TipoCuentaEntity entity) {
        try {
            return TipoCuenta.reconstruir(
                entity.getId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.isActivo());
        } catch (DomainException ex) {
            throw new DatosPersistidosInvalidos(
                "El tipo de cuenta almacenado no cumple las invariantes de dominio.",
                ex);
        }
    }
}
