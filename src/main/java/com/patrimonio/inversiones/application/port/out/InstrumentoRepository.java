package com.patrimonio.inversiones.application.port.out;

import com.patrimonio.inversiones.application.query.FiltroInstrumentos;
import com.patrimonio.inversiones.domain.model.Instrumento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del catálogo de instrumentos.
 *
 * <p>Devuelve agregados de dominio, nunca entidades JPA. Los instrumentos no
 * se eliminan: el estado se cambia mediante {@link #guardar(Instrumento)}.</p>
 */
public interface InstrumentoRepository {

    List<Instrumento> listar(FiltroInstrumentos filtro);

    Optional<Instrumento> buscarPorId(UUID instrumentoId);

    List<Instrumento> buscarPorIds(Collection<UUID> instrumentoIds);

    /**
     * Indica si el símbolo ya existe para la moneda, excluyendo el instrumento
     * indicado cuando se está editando.
     */
    boolean existeSimboloEnMoneda(
        String simbolo,
        UUID monedaId,
        UUID excluirInstrumentoId);

    /**
     * Indica si el ISIN ya existe en el catálogo, excluyendo el instrumento
     * indicado cuando se está editando.
     */
    boolean existeIsin(String isin, UUID excluirInstrumentoId);

    Instrumento guardar(Instrumento instrumento);
}
