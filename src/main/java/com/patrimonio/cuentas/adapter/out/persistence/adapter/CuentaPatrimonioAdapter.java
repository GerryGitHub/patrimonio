package com.patrimonio.cuentas.adapter.out.persistence.adapter;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.adapter.out.persistence.entity.InstitucionEntity;
import com.patrimonio.cuentas.adapter.out.persistence.entity.TipoCuentaEntity;
import com.patrimonio.cuentas.adapter.out.persistence.repository.CuentaFinancieraJpaRepository;
import com.patrimonio.cuentas.adapter.out.persistence.repository.InstitucionJpaRepository;
import com.patrimonio.cuentas.adapter.out.persistence.repository.TipoCuentaJpaRepository;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.cuentas.api.port.CuentaPatrimonioPort;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adaptador del contrato público de cuentas patrimoniales.
 *
 * <p>Resuelve los nombres de institución y tipo de cuenta en lote. No expone
 * números de cuenta ni saldos: el saldo de efectivo pertenece a un módulo que
 * todavía no existe.</p>
 */
@Component
public class CuentaPatrimonioAdapter implements CuentaPatrimonioPort {

    private final CuentaFinancieraJpaRepository cuentas;
    private final TipoCuentaJpaRepository tiposCuenta;
    private final InstitucionJpaRepository instituciones;

    public CuentaPatrimonioAdapter(
        CuentaFinancieraJpaRepository cuentas,
        TipoCuentaJpaRepository tiposCuenta,
        InstitucionJpaRepository instituciones
    ) {
        this.cuentas = cuentas;
        this.tiposCuenta = tiposCuenta;
        this.instituciones = instituciones;
    }

    @Override
    public List<CuentaPatrimonio> listarPorPortafolio(UUID portafolioId) {
        List<CuentaFinancieraEntity> encontradas =
            cuentas.listarPorPortafolio(portafolioId, null, null, null);
        if (encontradas.isEmpty()) {
            return List.of();
        }

        Set<UUID> tipoIds = encontradas.stream()
            .map(CuentaFinancieraEntity::getTipoCuentaId)
            .collect(Collectors.toSet());
        Map<UUID, String> nombresTipo = tiposCuenta.findAllById(tipoIds).stream()
            .collect(Collectors.toMap(
                TipoCuentaEntity::getId,
                TipoCuentaEntity::getNombre,
                (primero, segundo) -> primero));

        Set<UUID> institucionIds = encontradas.stream()
            .map(CuentaFinancieraEntity::getInstitucionId)
            .collect(Collectors.toSet());
        Map<UUID, String> nombresInstitucion =
            instituciones.findAllById(institucionIds).stream()
                .collect(Collectors.toMap(
                    InstitucionEntity::getId,
                    InstitucionEntity::getNombre,
                    (primero, segundo) -> primero));

        return encontradas.stream()
            .map(cuenta -> convertir(cuenta, nombresTipo, nombresInstitucion))
            .toList();
    }

    private CuentaPatrimonio convertir(
        CuentaFinancieraEntity cuenta,
        Map<UUID, String> nombresTipo,
        Map<UUID, String> nombresInstitucion
    ) {
        String tipo = nombresTipo.get(cuenta.getTipoCuentaId());
        if (tipo == null) {
            throw new DatosPersistidosInvalidos(
                "La cuenta " + cuenta.getId()
                    + " referencia un tipo de cuenta inexistente.");
        }
        String institucion = nombresInstitucion.get(cuenta.getInstitucionId());
        if (institucion == null) {
            throw new DatosPersistidosInvalidos(
                "La cuenta " + cuenta.getId()
                    + " referencia una institución inexistente.");
        }
        return new CuentaPatrimonio(
            cuenta.getId(),
            cuenta.getPortafolioId(),
            cuenta.getNombre(),
            cuenta.getInstitucionId(),
            institucion,
            cuenta.getTipoCuentaId(),
            tipo,
            cuenta.getMonedaId(),
            cuenta.isActivo());
    }
}
