package com.patrimonio.consolidacion.application.support;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Autorización por membresía del módulo de consolidación.
 *
 * <p>La ausencia de membresía se trata como recurso inexistente para no
 * revelar portafolios ajenos. El endpoint global solo usa
 * {@link #listarAutorizados(UUID)}; nunca recorre portafolios sin membresía.</p>
 */
@Component
public class AccesoPatrimonio {

    private final AccesoQueryPort accesoQueryPort;

    public AccesoPatrimonio(AccesoQueryPort accesoQueryPort) {
        this.accesoQueryPort = accesoQueryPort;
    }

    public PermisoPortafolio exigirLectura(UUID usuarioId, UUID portafolioId) {
        PermisoPortafolio permiso = accesoQueryPort
            .buscarPermiso(usuarioId, portafolioId)
            .orElseThrow(() -> new NotFoundException(
                "No existe el portafolio indicado."));
        if (!permiso.lectura()) {
            throw new AccesoDenegadoException();
        }
        return permiso;
    }

    public PermisoPortafolio exigirEscritura(UUID usuarioId, UUID portafolioId) {
        PermisoPortafolio permiso = exigirLectura(usuarioId, portafolioId);
        if (!permiso.escritura()) {
            throw new AccesoDenegadoException();
        }
        return permiso;
    }

    public List<PortafolioResumen> listarAutorizados(UUID usuarioId) {
        return accesoQueryPort.listarAutorizados(usuarioId);
    }
}
