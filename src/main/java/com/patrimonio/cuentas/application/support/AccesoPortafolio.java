package com.patrimonio.cuentas.application.support;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Comprueba la capacidad del usuario autenticado sobre un portafolio.
 *
 * <p>La ausencia de membresía se trata como recurso inexistente para no
 * revelar portafolios ajenos; una membresía sin la capacidad requerida se
 * traduce en acceso denegado. Se centraliza aquí para que todos los casos de
 * uso del módulo apliquen la misma regla.</p>
 */
@Component
public class AccesoPortafolio {

    private final AccesoQueryPort accesoQueryPort;

    public AccesoPortafolio(AccesoQueryPort accesoQueryPort) {
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
}
