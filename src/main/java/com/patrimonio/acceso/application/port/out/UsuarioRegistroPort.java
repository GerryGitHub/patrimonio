package com.patrimonio.acceso.application.port.out;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;

/**
 * Persistencia de nuevas cuentas de usuario.
 *
 * <p>El correo llega normalizado (sin espacios exteriores y en minúsculas).
 * La implementación debe traducir una violación de unicidad a
 * {@link com.patrimonio.acceso.application.error.EmailDuplicadoException} para
 * cubrir la carrera entre la comprobación y la inserción.</p>
 */
public interface UsuarioRegistroPort {

    boolean existePorEmail(String emailNormalizado);

    UsuarioAutenticado crear(
        String nombre,
        String emailNormalizado,
        String passwordHash);
}
