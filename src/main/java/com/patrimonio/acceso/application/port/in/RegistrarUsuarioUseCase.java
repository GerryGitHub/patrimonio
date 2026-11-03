package com.patrimonio.acceso.application.port.in;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.RegistrarUsuarioCommand;

/**
 * Caso de uso de registro de usuarios.
 *
 * <p>No crea una sesión: el alta y el inicio de sesión son operaciones
 * independientes. El adaptador web decide si redirige al login o si, en el
 * futuro, encadena una autenticación explícita.</p>
 */
public interface RegistrarUsuarioUseCase {

    UsuarioAutenticado execute(RegistrarUsuarioCommand command);
}
