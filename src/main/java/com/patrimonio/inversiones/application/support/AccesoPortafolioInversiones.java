package com.patrimonio.inversiones.application.support;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Comprueba la capacidad del usuario autenticado sobre un portafolio.
 *
 * <p>Es una copia deliberada de la regla del módulo {@code cuentas}: la
 * ausencia de membresía se trata como recurso inexistente para no revelar
 * portafolios ajenos; una membresía sin la capacidad requerida se traduce en
 * acceso denegado. Mantenerla en el módulo evita acoplar inversiones a las
 * clases internas de otro módulo.</p>
 */
@Component
public class AccesoPortafolioInversiones {

    private final AccesoQueryPort accesoQueryPort;

    public AccesoPortafolioInversiones(AccesoQueryPort accesoQueryPort) {
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
