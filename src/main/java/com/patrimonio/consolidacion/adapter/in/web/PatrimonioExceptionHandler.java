package com.patrimonio.consolidacion.adapter.in.web;

import com.patrimonio.consolidacion.domain.error.SnapshotInvalidoException;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import com.patrimonio.shared.domain.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones del Centro de Patrimonio a RFC 9457
 * {@link ProblemDetail} con un código estable.
 *
 * <p>Los detalles internos y las trazas no se exponen al cliente; solo se
 * registran en el servidor.</p>
 */
@RestControllerAdvice(basePackageClasses = {
    PatrimonioController.class,
    PatrimonioGlobalController.class
})
public class PatrimonioExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(PatrimonioExceptionHandler.class);

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class
    })
    public ProblemDetail handleParametroInvalido(Exception ex) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "PARAMETRO_INVALIDO",
            "La consulta contiene un parámetro con formato desconocido.");
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class
    })
    public ProblemDetail handleSolicitudInvalida(Exception ex) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "VALIDACION_ENTRADA",
            "La solicitud no cumple el formato esperado.");
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNoEncontrado(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail handleAccesoDenegado(AccesoDenegadoException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(SnapshotInvalidoException.class)
    public ProblemDetail handleSnapshotInvalido(SnapshotInvalidoException ex) {
        return problem(
            HttpStatus.UNPROCESSABLE_CONTENT, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler({
        ObjectOptimisticLockingFailureException.class,
        DataIntegrityViolationException.class
    })
    public ProblemDetail handleConflictoDeConcurrencia(Exception ex) {
        log.warn("Conflicto de concurrencia o integridad en consolidación", ex);
        return problem(
            HttpStatus.CONFLICT,
            "CONFLICTO_CONCURRENCIA",
            "La operación chocó con otro cambio simultáneo. Vuelve a intentarlo.");
    }

    @ExceptionHandler(DatosPersistidosInvalidos.class)
    public ProblemDetail handleDatosInconsistentes(DatosPersistidosInvalidos ex) {
        log.error("Datos persistidos inconsistentes en consolidación", ex);
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ex.codigo(),
            "Los datos almacenados no son coherentes.");
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleErrorDeDominio(DomainException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErrorInesperado(Exception ex) {
        log.error("Error no controlado en consolidación", ex);
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "ERROR_INTERNO",
            "Se produjo un error interno.");
    }

    private static ProblemDetail problem(
        HttpStatusCode status,
        String codigo,
        String detalle
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalle);
        problem.setTitle(tituloPara(status));
        problem.setProperty("code", codigo);
        return problem;
    }

    private static String tituloPara(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Solicitud inválida";
            case 403 -> "Acceso denegado";
            case 404 -> "Recurso no encontrado";
            case 409 -> "Conflicto";
            case 422 -> "Regla de negocio incumplida";
            case 500 -> "Error interno";
            default -> "Error HTTP";
        };
    }
}
