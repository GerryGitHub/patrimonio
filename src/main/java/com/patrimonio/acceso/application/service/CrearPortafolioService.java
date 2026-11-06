package com.patrimonio.acceso.application.service;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.application.command.CrearPortafolioCommand;
import com.patrimonio.acceso.application.error.MonedaInvalidaException;
import com.patrimonio.acceso.application.error.PortafolioInvalidoException;
import com.patrimonio.acceso.application.port.in.CrearPortafolioUseCase;
import com.patrimonio.acceso.application.port.out.MonedaQueryPort;
import com.patrimonio.acceso.application.port.out.PortafolioRegistroPort;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso de creación de portafolios.
 *
 * <p>Normaliza el nombre y la descripción, valida los límites del esquema y
 * comprueba que la moneda base exista y esté activa. La persistencia del
 * portafolio y de la membresía {@code PROPIETARIO} ocurre en una sola
 * transacción: si falla cualquiera de las dos inserciones, no queda un
 * portafolio huérfano.</p>
 */
@Service
public class CrearPortafolioService implements CrearPortafolioUseCase {

    private static final int NOMBRE_MAXIMO = 150;
    private static final int DESCRIPCION_MAXIMA = 500;

    private final MonedaQueryPort monedas;
    private final PortafolioRegistroPort portafolios;

    public CrearPortafolioService(
        MonedaQueryPort monedas,
        PortafolioRegistroPort portafolios
    ) {
        this.monedas = monedas;
        this.portafolios = portafolios;
    }

    @Override
    @Transactional
    public PortafolioResumen execute(CrearPortafolioCommand command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.usuarioId(), "usuarioId");

        String nombre = normalizar(command.nombre());
        String descripcion = normalizarDescripcion(command.descripcion());

        validar(nombre, descripcion, command.monedaBaseId());

        MonedaResumen moneda = monedas
            .buscarActivaPorId(command.monedaBaseId())
            .orElseThrow(MonedaInvalidaException::new);

        return portafolios.crear(
            command.usuarioId(),
            nombre,
            descripcion,
            moneda);
    }

    private static void validar(
        String nombre,
        String descripcion,
        java.util.UUID monedaBaseId
    ) {
        if (nombre.isEmpty() || nombre.length() > NOMBRE_MAXIMO) {
            throw new PortafolioInvalidoException();
        }
        if (descripcion != null && descripcion.length() > DESCRIPCION_MAXIMA) {
            throw new PortafolioInvalidoException();
        }
        if (monedaBaseId == null) {
            throw new PortafolioInvalidoException();
        }
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.strip();
    }

    private static String normalizarDescripcion(String valor) {
        if (valor == null) {
            return null;
        }
        String descripcion = valor.strip();
        return descripcion.isEmpty() ? null : descripcion;
    }
}
