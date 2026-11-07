package com.patrimonio.cuentas.application.port.in;

import com.patrimonio.cuentas.application.query.ListarInstitucionesQuery;
import com.patrimonio.cuentas.application.view.InstitucionView;
import java.util.List;

/**
 * Caso de uso: listar el catálogo de instituciones.
 */
public interface ListarInstitucionesUseCase {

    List<InstitucionView> execute(ListarInstitucionesQuery query);
}
