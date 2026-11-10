package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.CrearPosicionCommand;
import com.patrimonio.inversiones.application.port.in.CrearPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.PosicionRepository;
import com.patrimonio.inversiones.application.support.AccesoPortafolioInversiones;
import com.patrimonio.inversiones.application.support.FechasInversiones;
import com.patrimonio.inversiones.application.support.InversionesVistas;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.support.ValidadorCompatibilidadCuentaInstrumento;
import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.domain.error.PosicionInvalidaException;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del alta de una posición inicial.
 *
 * <p>La posición pertenece a una cuenta del portafolio de la ruta. Se valida
 * que la cuenta exista dentro de ese portafolio, que esté activa y que sea
 * compatible con la clase de activo del instrumento. La moneda se deriva del
 * instrumento y no se acepta desde el cliente.</p>
 */
@Service
public class CrearPosicionService implements CrearPosicionUseCase {

    private final AccesoPortafolioInversiones accesoPortafolio;
    private final CuentaConsultaPort cuentas;
    private final InstrumentoRepository instrumentos;
    private final CatalogoInstrumentoRepository catalogo;
    private final PosicionRepository posiciones;
    private final ValidadorCompatibilidadCuentaInstrumento compatibilidad;
    private final InversionesVistas vistas;

    public CrearPosicionService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        CatalogoInstrumentoRepository catalogo,
        PosicionRepository posiciones,
        ValidadorCompatibilidadCuentaInstrumento compatibilidad,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.catalogo = catalogo;
        this.posiciones = posiciones;
        this.compatibilidad = compatibilidad;
        this.vistas = vistas;
    }

    @Override
    @Transactional
    public PosicionView execute(CrearPosicionCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        CuentaResumen cuenta = cuentas
            .buscarEnPortafolio(command.portafolioId(), command.cuentaId())
            .orElseThrow(() -> new NotFoundException(
                "No existe la cuenta indicada en el portafolio."));

        Instrumento instrumento = instrumentos
            .buscarPorId(command.instrumentoId())
            .orElseThrow(() -> new NotFoundException(
                "No existe el instrumento indicado."));

        if (!instrumento.activo()) {
            throw new PosicionInvalidaException(
                "El instrumento indicado está inactivo y no admite posiciones nuevas.");
        }

        compatibilidad.exigirCuentaActiva(cuenta);
        compatibilidad.validar(cuenta, claseDe(instrumento));

        LocalDate fecha = command.fechaApertura() == null
            ? FechasInversiones.hoy()
            : command.fechaApertura();

        Posicion posicion = Posicion.crear(
            UUID.randomUUID(),
            cuenta.id(),
            instrumento.id(),
            command.cantidad(),
            command.costoUnitario(),
            instrumento.monedaId(),
            TextosInversiones.opcional(command.observaciones()),
            FechasInversiones.inicioDelDia(fecha),
            Instant.now());

        return vistas.posicion(
            posiciones.guardar(posicion),
            vistas.instrumento(instrumento),
            cuenta);
    }

    private ClaseActivo claseDe(Instrumento instrumento) {
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
