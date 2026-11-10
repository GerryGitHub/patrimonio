package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.InstrumentoInvalidoException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Instrumento financiero del catálogo global.
 *
 * <p>Agregado de dominio puro: no depende de Spring, JPA, Hibernate ni
 * Jackson. La clase de activo y el tipo de instrumento se referencian por
 * identificador, sin asociaciones de objetos. El ISIN se modela como un dato
 * del instrumento y el adaptador lo persiste en
 * {@code identificador_instrumento}.</p>
 *
 * <p>El catálogo es compartido: no pertenece a un usuario ni a un portafolio.
 * Cualquier usuario autenticado puede consultarlo y administrarlo.</p>
 */
public final class Instrumento {

    private static final int LONGITUD_MAXIMA_NOMBRE = 200;
    private static final int LONGITUD_MAXIMA_SIMBOLO = 32;
    private static final int LONGITUD_MAXIMA_EMISOR = 150;
    private static final int LONGITUD_MAXIMA_MERCADO = 100;
    private static final int LONGITUD_MAXIMA_IDENTIFICADOR_EXTERNO = 100;
    private static final int LONGITUD_MAXIMA_ISIN = 100;
    private static final int LONGITUD_MAXIMA_DESCRIPCION = 500;
    private static final Pattern PATRON_ISIN = Pattern.compile("^[A-Z0-9]{12}$");

    private final UUID id;
    private final UUID tipoInstrumentoId;
    private final String nombre;
    private final Optional<String> simbolo;
    private final Optional<String> emisor;
    private final Optional<String> mercado;
    private final UUID monedaId;
    private final Optional<String> identificadorExterno;
    private final Optional<String> isin;
    private final Optional<String> descripcion;
    private final boolean activo;
    private final Instant fechaCreacion;
    private final Instant fechaActualizacion;

    private Instrumento(
        UUID id,
        UUID tipoInstrumentoId,
        String nombre,
        String simbolo,
        String emisor,
        String mercado,
        UUID monedaId,
        String identificadorExterno,
        String isin,
        String descripcion,
        boolean activo,
        Instant fechaCreacion,
        Instant fechaActualizacion
    ) {
        this.id = id;
        this.tipoInstrumentoId = tipoInstrumentoId;
        this.nombre = nombre;
        this.simbolo = Optional.ofNullable(simbolo);
        this.emisor = Optional.ofNullable(emisor);
        this.mercado = Optional.ofNullable(mercado);
        this.monedaId = monedaId;
        this.identificadorExterno = Optional.ofNullable(identificadorExterno);
        this.isin = Optional.ofNullable(isin);
        this.descripcion = Optional.ofNullable(descripcion);
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    /**
     * Crea un instrumento nuevo y activo.
     */
    public static Instrumento crear(
        UUID id,
        UUID tipoInstrumentoId,
        String nombre,
        String simbolo,
        String emisor,
        String mercado,
        UUID monedaId,
        String identificadorExterno,
        String isin,
        String descripcion,
        Instant ahora
    ) {
        requerir(id, "id");
        requerir(tipoInstrumentoId, "tipoInstrumentoId");
        requerir(monedaId, "monedaId");
        requerir(ahora, "ahora");
        validarNombre(nombre);
        validarOpcional(simbolo, "El símbolo", LONGITUD_MAXIMA_SIMBOLO);
        validarOpcional(emisor, "El emisor", LONGITUD_MAXIMA_EMISOR);
        validarOpcional(mercado, "El mercado", LONGITUD_MAXIMA_MERCADO);
        validarOpcional(
            identificadorExterno,
            "El identificador externo",
            LONGITUD_MAXIMA_IDENTIFICADOR_EXTERNO);
        validarOpcional(descripcion, "La descripción", LONGITUD_MAXIMA_DESCRIPCION);
        String isinNormalizado = normalizarIsin(isin);

        return new Instrumento(
            id,
            tipoInstrumentoId,
            nombre,
            simbolo,
            emisor,
            mercado,
            monedaId,
            identificadorExterno,
            isinNormalizado,
            descripcion,
            true,
            ahora,
            ahora
        );
    }

    /**
     * Reconstruye un instrumento desde persistencia validando sus invariantes.
     */
    public static Instrumento reconstruir(
        UUID id,
        UUID tipoInstrumentoId,
        String nombre,
        String simbolo,
        String emisor,
        String mercado,
        UUID monedaId,
        String identificadorExterno,
        String isin,
        String descripcion,
        boolean activo,
        Instant fechaCreacion,
        Instant fechaActualizacion
    ) {
        requerir(id, "id");
        requerir(tipoInstrumentoId, "tipoInstrumentoId");
        requerir(monedaId, "monedaId");
        requerir(fechaCreacion, "fechaCreacion");
        requerir(fechaActualizacion, "fechaActualizacion");
        validarNombre(nombre);
        validarOpcional(simbolo, "El símbolo", LONGITUD_MAXIMA_SIMBOLO);
        validarOpcional(emisor, "El emisor", LONGITUD_MAXIMA_EMISOR);
        validarOpcional(mercado, "El mercado", LONGITUD_MAXIMA_MERCADO);
        validarOpcional(
            identificadorExterno,
            "El identificador externo",
            LONGITUD_MAXIMA_IDENTIFICADOR_EXTERNO);
        validarOpcional(descripcion, "La descripción", LONGITUD_MAXIMA_DESCRIPCION);
        String isinNormalizado = normalizarIsin(isin);

        return new Instrumento(
            id,
            tipoInstrumentoId,
            nombre,
            simbolo,
            emisor,
            mercado,
            monedaId,
            identificadorExterno,
            isinNormalizado,
            descripcion,
            activo,
            fechaCreacion,
            fechaActualizacion
        );
    }

    /**
     * Devuelve una copia con los datos editables actualizados.
     *
     * <p>El identificador y el estado no cambian por esta vía: el estado tiene
     * su propia operación explícita.</p>
     */
    public Instrumento actualizarDatos(
        UUID tipoInstrumentoId,
        String nombre,
        String simbolo,
        String emisor,
        String mercado,
        UUID monedaId,
        String identificadorExterno,
        String isin,
        String descripcion,
        Instant ahora
    ) {
        requerir(tipoInstrumentoId, "tipoInstrumentoId");
        requerir(monedaId, "monedaId");
        requerir(ahora, "ahora");
        validarNombre(nombre);
        validarOpcional(simbolo, "El símbolo", LONGITUD_MAXIMA_SIMBOLO);
        validarOpcional(emisor, "El emisor", LONGITUD_MAXIMA_EMISOR);
        validarOpcional(mercado, "El mercado", LONGITUD_MAXIMA_MERCADO);
        validarOpcional(
            identificadorExterno,
            "El identificador externo",
            LONGITUD_MAXIMA_IDENTIFICADOR_EXTERNO);
        validarOpcional(descripcion, "La descripción", LONGITUD_MAXIMA_DESCRIPCION);
        String isinNormalizado = normalizarIsin(isin);

        return new Instrumento(
            id,
            tipoInstrumentoId,
            nombre,
            simbolo,
            emisor,
            mercado,
            monedaId,
            identificadorExterno,
            isinNormalizado,
            descripcion,
            activo,
            fechaCreacion,
            ahora
        );
    }

    /**
     * Activa o desactiva el instrumento conservando su historial.
     */
    public Instrumento cambiarEstado(boolean nuevoActivo, Instant ahora) {
        requerir(ahora, "ahora");
        return new Instrumento(
            id,
            tipoInstrumentoId,
            nombre,
            simbolo.orElse(null),
            emisor.orElse(null),
            mercado.orElse(null),
            monedaId,
            identificadorExterno.orElse(null),
            isin.orElse(null),
            descripcion.orElse(null),
            nuevoActivo,
            fechaCreacion,
            ahora
        );
    }

    private static void requerir(Object valor, String campo) {
        if (valor == null) {
            throw new InstrumentoInvalidoException(
                "El campo '" + campo + "' es obligatorio.");
        }
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new InstrumentoInvalidoException(
                "El nombre del instrumento es obligatorio.");
        }
        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new InstrumentoInvalidoException(
                "El nombre del instrumento no puede superar "
                    + LONGITUD_MAXIMA_NOMBRE + " caracteres.");
        }
    }

    private static void validarOpcional(
        String valor,
        String etiqueta,
        int longitudMaxima
    ) {
        if (valor != null && valor.length() > longitudMaxima) {
            throw new InstrumentoInvalidoException(
                etiqueta + " no puede superar " + longitudMaxima + " caracteres.");
        }
    }

    private static String normalizarIsin(String isin) {
        if (isin == null) {
            return null;
        }
        String normalizado = isin.strip().toUpperCase(Locale.ROOT);
        if (normalizado.isEmpty()) {
            return null;
        }
        if (normalizado.length() > LONGITUD_MAXIMA_ISIN
            || !PATRON_ISIN.matcher(normalizado).matches()) {
            throw new InstrumentoInvalidoException(
                "El ISIN debe tener 12 caracteres alfanuméricos.");
        }
        return normalizado;
    }

    public UUID id() {
        return id;
    }

    public UUID tipoInstrumentoId() {
        return tipoInstrumentoId;
    }

    public String nombre() {
        return nombre;
    }

    public Optional<String> simbolo() {
        return simbolo;
    }

    public Optional<String> emisor() {
        return emisor;
    }

    public Optional<String> mercado() {
        return mercado;
    }

    public UUID monedaId() {
        return monedaId;
    }

    public Optional<String> identificadorExterno() {
        return identificadorExterno;
    }

    public Optional<String> isin() {
        return isin;
    }

    public Optional<String> descripcion() {
        return descripcion;
    }

    public boolean activo() {
        return activo;
    }

    public Instant fechaCreacion() {
        return fechaCreacion;
    }

    public Instant fechaActualizacion() {
        return fechaActualizacion;
    }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof Instrumento instrumento && id.equals(instrumento.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
