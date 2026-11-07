package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.command.CrearInstitucionCommand;
import com.patrimonio.cuentas.application.port.in.CrearInstitucionUseCase;
import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.application.support.Textos;
import com.patrimonio.cuentas.application.view.InstitucionView;
import com.patrimonio.cuentas.domain.error.InstitucionDuplicadaException;
import com.patrimonio.cuentas.domain.error.InstitucionInvalidaException;
import com.patrimonio.cuentas.domain.model.Institucion;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del alta de instituciones financieras.
 *
 * <p>Normaliza el nombre, rechaza duplicados sin distinguir mayúsculas y crea
 * la institución activa. El catálogo es global: cualquier usuario autenticado
 * puede reutilizarlo desde sus cuentas.</p>
 */
@Service
public class CrearInstitucionService implements CrearInstitucionUseCase {

    private static final int NOMBRE_MAXIMO = 150;

    private final InstitucionRepository instituciones;

    public CrearInstitucionService(InstitucionRepository instituciones) {
        this.instituciones = instituciones;
    }

    @Override
    @Transactional
    public InstitucionView execute(CrearInstitucionCommand command) {
        Objects.requireNonNull(command, "command");

        String nombre = Textos.nombre(command.nombre());
        if (nombre.isEmpty() || nombre.length() > NOMBRE_MAXIMO) {
            throw new InstitucionInvalidaException(
                "El nombre de la institución es obligatorio y no puede superar "
                    + NOMBRE_MAXIMO + " caracteres.");
        }
        if (command.tipo() == null) {
            throw new InstitucionInvalidaException(
                "La categoría de la institución es obligatoria.");
        }
        if (instituciones.existePorNombre(nombre)) {
            throw new InstitucionDuplicadaException(
                "Ya existe una institución con ese nombre.");
        }

        Institucion institucion = Institucion.crear(
            UUID.randomUUID(),
            nombre,
            command.tipo(),
            Textos.opcional(command.pais()),
            Textos.opcional(command.sitioWeb()));

        return InstitucionView.from(instituciones.guardar(institucion));
    }
}
