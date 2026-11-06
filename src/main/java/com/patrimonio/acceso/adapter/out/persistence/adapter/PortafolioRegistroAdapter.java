package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.entity.MembresiaEntity;
import com.patrimonio.acceso.adapter.out.persistence.entity.PortafolioEntity;
import com.patrimonio.acceso.adapter.out.persistence.repository.MembresiaJpaRepository;
import com.patrimonio.acceso.adapter.out.persistence.repository.PortafolioJpaRepository;
import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.application.port.out.PortafolioRegistroPort;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que crea un portafolio y la membresía de su creador.
 *
 * <p>Las dos inserciones comparten la transacción del caso de uso. El rol
 * inicial es {@code PROPIETARIO}; la restricción única de {@code membresia}
 * impide duplicar la relación usuario-portafolio.</p>
 */
@Component
public class PortafolioRegistroAdapter implements PortafolioRegistroPort {

    private static final String ROL_PROPIETARIO = "PROPIETARIO";

    private final PortafolioJpaRepository portafolios;
    private final MembresiaJpaRepository membresias;

    public PortafolioRegistroAdapter(
        PortafolioJpaRepository portafolios,
        MembresiaJpaRepository membresias
    ) {
        this.portafolios = portafolios;
        this.membresias = membresias;
    }

    @Override
    public PortafolioResumen crear(
        UUID usuarioId,
        String nombre,
        String descripcion,
        MonedaResumen monedaBase
    ) {
        UUID portafolioId = UUID.randomUUID();
        Instant ahora = Instant.now();

        portafolios.save(new PortafolioEntity(
            portafolioId,
            nombre,
            monedaBase.id(),
            descripcion,
            true,
            ahora,
            ahora));

        membresias.save(new MembresiaEntity(
            UUID.randomUUID(),
            usuarioId,
            portafolioId,
            ROL_PROPIETARIO,
            ahora));

        return new PortafolioResumen(
            portafolioId,
            nombre,
            descripcion,
            monedaBase,
            true);
    }
}
