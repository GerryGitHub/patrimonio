package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.repository.MembresiaJpaRepository;
import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que responde consultas de autorización por membresía.
 *
 * <p>No revela el rol ni los datos internos de la membresía: solo confirma el
 * permiso y lista los portafolios activos autorizados.</p>
 */
@Component
public class AccesoQueryAdapter implements AccesoQueryPort {

    private final MembresiaJpaRepository repository;

    public AccesoQueryAdapter(MembresiaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PermisoPortafolio> buscarPermiso(UUID usuarioId, UUID portafolioId) {
        return repository.buscarPortafolioAutorizado(usuarioId, portafolioId)
            .map(id -> new PermisoPortafolio(id, true));
    }

    @Override
    public List<PortafolioResumen> listarAutorizados(UUID usuarioId) {
        return repository.listarPortafoliosAutorizados(usuarioId);
    }
}
