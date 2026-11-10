package com.patrimonio.inversiones.application.support;

import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.domain.error.InstrumentoDuplicadoException;
import com.patrimonio.inversiones.domain.error.InstrumentoInvalidoException;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Valida las relaciones y la unicidad de un instrumento.
 *
 * <p>El tipo de instrumento debe existir y estar activo; la moneda debe
 * existir y estar activa. El símbolo se compara dentro de la misma moneda y el
 * ISIN es único en todo el catálogo.</p>
 */
@Component
public class ValidadorRelacionesInstrumento {

    private final CatalogoInstrumentoRepository catalogo;
    private final MonedaCatalogoPort monedas;
    private final InstrumentoRepository instrumentos;

    public ValidadorRelacionesInstrumento(
        CatalogoInstrumentoRepository catalogo,
        MonedaCatalogoPort monedas,
        InstrumentoRepository instrumentos
    ) {
        this.catalogo = catalogo;
        this.monedas = monedas;
        this.instrumentos = instrumentos;
    }

    public TipoInstrumento validarTipoYMoneda(
        UUID tipoInstrumentoId,
        UUID monedaId
    ) {
        if (tipoInstrumentoId == null) {
            throw new InstrumentoInvalidoException(
                "El tipo de instrumento es obligatorio.");
        }
        TipoInstrumento tipo = catalogo.buscarTipoActivoPorId(tipoInstrumentoId)
            .orElseThrow(() -> new InstrumentoInvalidoException(
                "El tipo de instrumento indicado no existe o no está activo."));

        if (monedaId == null) {
            throw new InstrumentoInvalidoException(
                "La moneda del instrumento es obligatoria.");
        }
        if (monedas.buscarActivaPorId(monedaId).isEmpty()) {
            throw new InstrumentoInvalidoException(
                "La moneda indicada no existe o no está activa.");
        }
        return tipo;
    }

    public void validarUnicidad(
        String simbolo,
        UUID monedaId,
        String isin,
        UUID excluirInstrumentoId
    ) {
        if (simbolo != null && instrumentos.existeSimboloEnMoneda(
            simbolo, monedaId, excluirInstrumentoId)) {
            throw new InstrumentoDuplicadoException(
                "Ya existe un instrumento con ese símbolo en la misma moneda.");
        }
        if (isin != null && instrumentos.existeIsin(isin, excluirInstrumentoId)) {
            throw new InstrumentoDuplicadoException(
                "Ya existe un instrumento con ese ISIN.");
        }
    }
}
