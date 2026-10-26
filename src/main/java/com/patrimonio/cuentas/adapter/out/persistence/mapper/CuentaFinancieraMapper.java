package com.patrimonio.cuentas.adapter.out.persistence.mapper;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre la entidad JPA y el agregado de dominio.
 *
 * <p>Si la fila almacenada no permite reconstruir un agregado válido, se lanza
 * {@link DatosPersistidosInvalidos} en lugar de devolver datos incoherentes.</p>
 */
@Component
public class CuentaFinancieraMapper {

    public CuentaFinanciera toDomain(CuentaFinancieraEntity entity) {
        try {
            NumeroCuenta numeroCuenta = entity.getNumeroCuenta() == null
                ? null
                : new NumeroCuenta(entity.getNumeroCuenta());

            return CuentaFinanciera.reconstruir(
                entity.getId(),
                entity.getPortafolioId(),
                entity.getInstitucionId(),
                entity.getTipoCuentaId(),
                entity.getNombre(),
                numeroCuenta,
                entity.getMonedaId(),
                entity.isActivo(),
                entity.getFechaApertura(),
                entity.getFechaCierre()
            );
        } catch (DomainException ex) {
            throw new DatosPersistidosInvalidos(
                "La cuenta financiera almacenada no cumple las invariantes de dominio.",
                ex);
        }
    }
}
