package com.patrimonio.inversiones.application.port.out;

import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del catálogo de clases y tipos de instrumento.
 *
 * <p>La clasificación es extensible: el módulo no tiene enumeraciones fijas
 * de categorías ni de tipos. Las reglas de compatibilidad con cuentas usan los
 * nombres del catálogo de forma conservadora y documentada.</p>
 */
public interface CatalogoInstrumentoRepository {

    List<ClaseActivo> listarClasesActivas();

    Optional<ClaseActivo> buscarClasePorId(UUID claseActivoId);

    List<ClaseActivo> buscarClasesPorIds(Collection<UUID> claseActivoIds);

    List<TipoInstrumento> listarTiposActivos();

    List<TipoInstrumento> listarTiposActivosPorClase(UUID claseActivoId);

    List<TipoInstrumento> buscarTiposPorIds(Collection<UUID> tipoInstrumentoIds);

    Optional<TipoInstrumento> buscarTipoActivoPorId(UUID tipoInstrumentoId);

    Optional<TipoInstrumento> buscarTipoPorId(UUID tipoInstrumentoId);
}
