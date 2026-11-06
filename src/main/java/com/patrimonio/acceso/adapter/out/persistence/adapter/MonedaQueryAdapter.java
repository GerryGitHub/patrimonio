package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.entity.MonedaEntity;
import com.patrimonio.acceso.adapter.out.persistence.repository.MonedaJpaRepository;
import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.application.port.out.MonedaQueryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que lee el catálogo de monedas activas.
 */
@Component
public class MonedaQueryAdapter implements MonedaQueryPort {

    private final MonedaJpaRepository repository;

    public MonedaQueryAdapter(MonedaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<MonedaResumen> listarActivas() {
        return repository.findByActivoTrueOrderByCodigoAsc().stream()
            .map(MonedaQueryAdapter::toResumen)
            .toList();
    }

    @Override
    public Optional<MonedaResumen> buscarActivaPorId(UUID monedaId) {
        return repository.findByIdAndActivoTrue(monedaId)
            .map(MonedaQueryAdapter::toResumen);
    }

    private static MonedaResumen toResumen(MonedaEntity moneda) {
        return new MonedaResumen(
            moneda.getId(),
            moneda.getCodigo(),
            moneda.getNombre(),
            moneda.getSimbolo(),
            moneda.getDecimales());
    }
}
