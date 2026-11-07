package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.entity.MembresiaEntity;
import com.patrimonio.acceso.adapter.out.persistence.repository.MembresiaJpaRepository;
import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que responde consultas de autorización por membresía.
 *
 * <p>No revela el rol ni los datos internos de la membresía: solo confirma el
 * permiso y lista los portafolios activos autorizados. La capacidad de
 * escritura se deriva del rol sin exponerlo al módulo consumidor.</p>
 */
@Component
public class AccesoQueryAdapter implements AccesoQueryPort {

    private static final Set<String> ROLES_CON_ESCRITURA =
        Set.of("PROPIETARIO", "ADMINISTRADOR", "EDITOR");

    private final MembresiaJpaRepository repository;

    public AccesoQueryAdapter(MembresiaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<PermisoPortafolio> buscarPermiso(UUID usuarioId, UUID portafolioId) {
        return repository.buscarMembresiaAutorizada(usuarioId, portafolioId)
            .map(AccesoQueryAdapter::toPermiso);
    }

    @Override
    public List<PortafolioResumen> listarAutorizados(UUID usuarioId) {
        return repository.listarPortafoliosAutorizados(usuarioId);
    }

    private static PermisoPortafolio toPermiso(MembresiaEntity membresia) {
        String rol = membresia.getRol() == null
            ? ""
            : membresia.getRol().strip().toUpperCase(Locale.ROOT);
        return new PermisoPortafolio(
            membresia.getPortafolioId(),
            true,
            ROLES_CON_ESCRITURA.contains(rol));
    }
}
