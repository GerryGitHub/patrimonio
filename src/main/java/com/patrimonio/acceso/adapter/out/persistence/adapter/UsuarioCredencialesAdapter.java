package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.repository.UsuarioJpaRepository;
import com.patrimonio.acceso.application.model.CredencialUsuario;
import com.patrimonio.acceso.application.port.out.UsuarioCredencialesPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que lee las credenciales almacenadas.
 *
 * <p>Es la única vía por la que la aplicación accede al hash de la contraseña.
 * No expone entidades JPA fuera de la capa de persistencia.</p>
 */
@Component
public class UsuarioCredencialesAdapter implements UsuarioCredencialesPort {

    private final UsuarioJpaRepository repository;

    public UsuarioCredencialesAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CredencialUsuario> buscarPorEmail(String emailNormalizado) {
        return repository.buscarPorEmail(emailNormalizado)
            .map(usuario -> new CredencialUsuario(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getPasswordHash(),
                usuario.isActivo()));
    }
}
