package com.patrimonio.cuentas.application.port.out;

import com.patrimonio.cuentas.domain.model.Institucion;
import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del catálogo de instituciones financieras.
 */
public interface InstitucionRepository {

    /**
     * Lista instituciones con filtros opcionales por categoría y estado.
     */
    List<Institucion> listar(TipoInstitucion tipo, Boolean activo);

    Optional<Institucion> buscarPorId(UUID institucionId);

    /**
     * Indica si ya existe una institución con ese nombre, sin distinguir
     * mayúsculas ni espacios exteriores.
     */
    boolean existePorNombre(String nombre);

    Institucion guardar(Institucion institucion);
}
