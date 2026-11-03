package com.patrimonio.acceso.adapter.out.persistence.adapter;

import com.patrimonio.acceso.adapter.out.persistence.entity.UsuarioEntity;
import com.patrimonio.acceso.adapter.out.persistence.repository.UsuarioJpaRepository;
import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.error.EmailDuplicadoException;
import com.patrimonio.acceso.application.port.out.UsuarioRegistroPort;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Adaptador de salida que crea usuarios en PostgreSQL.
 *
 * <p>La comprobación previa de duplicados da un error legible en el caso
 * normal. La restricción única de la base sigue siendo la fuente de verdad:
 * si dos registros concurrentes pasan la comprobación, la violación se
 * traduce a la misma excepción de negocio.</p>
 */
@Component
public class UsuarioRegistroAdapter implements UsuarioRegistroPort {

    private final UsuarioJpaRepository repository;

    public UsuarioRegistroAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existePorEmail(String emailNormalizado) {
        return repository.existsByEmailIgnoreCase(emailNormalizado);
    }

    @Override
    public UsuarioAutenticado crear(
        String nombre,
        String emailNormalizado,
        String passwordHash
    ) {
        UsuarioEntity usuario = UsuarioEntity.nuevo(
            UUID.randomUUID(), nombre, emailNormalizado, passwordHash);

        try {
            repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            if (esEmailDuplicado(ex)) {
                throw new EmailDuplicadoException();
            }
            throw ex;
        }

        return new UsuarioAutenticado(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNombre());
    }

    private static boolean esEmailDuplicado(DataIntegrityViolationException ex) {
        String mensaje = ex.getMostSpecificCause().getMessage();
        if (mensaje == null) {
            return false;
        }
        String normalizado = mensaje.toLowerCase(Locale.ROOT);
        return normalizado.contains("uq_usuario_email")
            || normalizado.contains("duplicate key");
    }
}
