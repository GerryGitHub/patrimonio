package com.patrimonio.inversiones.application.support;

import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import org.springframework.stereotype.Component;

/**
 * Resuelve la clase de activo de un instrumento a partir de su tipo.
 */
@Component
public class ResolutorClaseActivo {

    private final CatalogoInstrumentoRepository catalogo;

    public ResolutorClaseActivo(CatalogoInstrumentoRepository catalogo) {
        this.catalogo = catalogo;
    }

    public ClaseActivo de(Instrumento instrumento) {
        TipoInstrumento tipo = catalogo
            .buscarTipoPorId(instrumento.tipoInstrumentoId())
            .orElseThrow(() -> new DatosPersistidosInvalidos(
                "El instrumento " + instrumento.id()
                    + " referencia un tipo de instrumento inexistente."));
        return catalogo.buscarClasePorId(tipo.claseActivoId())
            .orElseThrow(() -> new DatosPersistidosInvalidos(
                "El tipo de instrumento " + tipo.id()
                    + " referencia una clase de activo inexistente."));
    }
}
