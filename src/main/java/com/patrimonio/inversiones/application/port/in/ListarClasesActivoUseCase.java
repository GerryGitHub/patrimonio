package com.patrimonio.inversiones.application.port.in;

import com.patrimonio.inversiones.application.view.ClaseActivoView;
import java.util.List;

/**
 * Caso de uso: consultar las clases de activo activas.
 */
public interface ListarClasesActivoUseCase {

    List<ClaseActivoView> execute();
}
