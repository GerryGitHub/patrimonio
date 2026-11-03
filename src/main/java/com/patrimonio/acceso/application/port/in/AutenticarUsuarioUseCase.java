package com.patrimonio.acceso.application.port.in;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.AutenticarUsuarioCommand;

/**
 * Caso de uso: verificar las credenciales de un usuario.
 *
 * <p>La creación de la sesión y la emisión de la cookie pertenecen al
 * adaptador de seguridad; este caso de uso solo decide si las credenciales son
 * válidas y devuelve la identidad mínima.</p>
 */
public interface AutenticarUsuarioUseCase {

    /**
     * @throws com.patrimonio.acceso.application.error.CredencialesInvalidasException
     *     si el usuario no existe, la contraseña no coincide o la cuenta está
     *     inactiva
     */
    UsuarioAutenticado execute(AutenticarUsuarioCommand command);
}
