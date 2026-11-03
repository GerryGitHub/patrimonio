package com.patrimonio.acceso.application.port.out;

import com.patrimonio.acceso.application.model.CredencialUsuario;
import java.util.Optional;

/**
 * Acceso de solo lectura a las credenciales almacenadas.
 *
 * <p>El correo se busca normalizado (sin espacios exteriores y en minúsculas).
 * Devuelve {@link Optional#empty()} si no existe; solo lanza excepciones ante
 * fallos de infraestructura.</p>
 */
public interface UsuarioCredencialesPort {

    Optional<CredencialUsuario> buscarPorEmail(String emailNormalizado);
}
