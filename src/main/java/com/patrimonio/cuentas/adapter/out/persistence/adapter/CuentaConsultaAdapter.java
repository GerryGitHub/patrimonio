package com.patrimonio.cuentas.adapter.out.persistence.adapter;

import com.patrimonio.cuentas.adapter.out.persistence.entity.CuentaFinancieraEntity;
import com.patrimonio.cuentas.adapter.out.persistence.entity.TipoCuentaEntity;
import com.patrimonio.cuentas.adapter.out.persistence.repository.CuentaFinancieraJpaRepository;
import com.patrimonio.cuentas.adapter.out.persistence.repository.TipoCuentaJpaRepository;
import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida del contrato público de consulta de cuentas.
 *
 * <p>Resuelve el nombre del tipo de cuenta en una sola consulta por listado,
 * evitando una consulta adicional por cada cuenta. No devuelve el número de
 * cuenta ni datos sensibles.</p>
 */
@Component
public class CuentaConsultaAdapter implements CuentaConsultaPort {

    private final CuentaFinancieraJpaRepository cuentas;
    private final TipoCuentaJpaRepository tiposCuenta;

    public CuentaConsultaAdapter(
        CuentaFinancieraJpaRepository cuentas,
        TipoCuentaJpaRepository tiposCuenta
    ) {
        this.cuentas = cuentas;
        this.tiposCuenta = tiposCuenta;
    }

    @Override
    public Optional<CuentaResumen> buscarPorId(UUID cuentaId) {
        return cuentas.findById(cuentaId).map(this::resumen);
    }

    @Override
    public Optional<CuentaResumen> buscarEnPortafolio(UUID portafolioId, UUID cuentaId) {
        return cuentas.findByIdAndPortafolioId(cuentaId, portafolioId)
            .map(this::resumen);
    }

    @Override
    public List<CuentaResumen> listarPorPortafolio(UUID portafolioId) {
        List<CuentaFinancieraEntity> entidades =
            cuentas.listarPorPortafolio(portafolioId, null, null, null);
        if (entidades.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> nombresTipo = tiposCuenta.findAll().stream()
            .collect(Collectors.toMap(
                TipoCuentaEntity::getId,
                TipoCuentaEntity::getNombre,
                (primero, segundo) -> primero));

        return entidades.stream()
            .map(cuenta -> resumen(cuenta, nombresTipo.get(cuenta.getTipoCuentaId())))
            .toList();
    }

    private CuentaResumen resumen(CuentaFinancieraEntity cuenta) {
        String nombreTipo = tiposCuenta.findById(cuenta.getTipoCuentaId())
            .map(TipoCuentaEntity::getNombre)
            .orElse(null);
        return resumen(cuenta, nombreTipo);
    }

    private CuentaResumen resumen(
        CuentaFinancieraEntity cuenta,
        String tipoCuentaNombre
    ) {
        return new CuentaResumen(
            cuenta.getId(),
            cuenta.getPortafolioId(),
            cuenta.getNombre(),
            cuenta.getInstitucionId(),
            cuenta.getTipoCuentaId(),
            tipoCuentaNombre,
            cuenta.getMonedaId(),
            cuenta.isActivo());
    }
}
