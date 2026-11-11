package com.patrimonio.inversiones.application.service;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.cuentas.api.port.CuentaConsultaPort;
import com.patrimonio.inversiones.application.command.CrearPosicionCommand;
import com.patrimonio.inversiones.application.port.in.CrearPosicionUseCase;
import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.InstrumentoRepository;
import com.patrimonio.inversiones.application.port.out.MovimientoRepository;
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
import com.patrimonio.inversiones.domain.model.MotorMovimientos;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.inversiones.domain.model.SolicitudMovimiento;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.math.BigDecimal;
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
    private final MovimientoRepository movimientos;
    private final MotorMovimientos motor;
    private final ValidadorCompatibilidadCuentaInstrumento compatibilidad;
    private final InversionesVistas vistas;

    public CrearPosicionService(
        AccesoPortafolioInversiones accesoPortafolio,
        CuentaConsultaPort cuentas,
        InstrumentoRepository instrumentos,
        CatalogoInstrumentoRepository catalogo,
        PosicionRepository posiciones,
        MovimientoRepository movimientos,
        MotorMovimientos motor,
        ValidadorCompatibilidadCuentaInstrumento compatibilidad,
        InversionesVistas vistas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.instrumentos = instrumentos;
        this.catalogo = catalogo;
        this.posiciones = posiciones;
        this.movimientos = movimientos;
        this.motor = motor;
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
        Instant ahora = Instant.now();
        String observaciones = TextosInversiones.opcional(command.observaciones());

        Posicion posicion = Posicion.crear(
            UUID.randomUUID(),
            cuenta.id(),
            instrumento.id(),
            command.cantidad(),
            command.costoUnitario(),
            instrumento.monedaId(),
            observaciones,
            FechasInversiones.inicioDelDia(fecha),
            ahora);

        Posicion creada = posiciones.guardar(posicion);

        // El alta de una posición genera su movimiento APERTURA en la misma
        // transacción. La posición ya nace con el estado resultante, por lo que
        // el movimiento se registra contra saldos cero y no se vuelve a aplicar.
        Movimiento apertura = Movimiento.registrar(
            new SolicitudMovimiento(
                UUID.randomUUID(),
                command.portafolioId(),
                cuenta.id(),
                creada.id(),
                instrumento.id(),
                TipoMovimiento.APERTURA,
                fecha,
                null,
                creada.cantidad(),
                creada.costoUnitario(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                instrumento.monedaId(),
                BigDecimal.ONE,
                null,
                observaciones,
                command.usuarioId(),
                null),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            motor,
            ahora);
        movimientos.guardar(apertura);

        return vistas.posicion(
            creada,
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
