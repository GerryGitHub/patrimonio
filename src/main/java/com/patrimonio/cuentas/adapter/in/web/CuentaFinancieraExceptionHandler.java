package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import com.patrimonio.shared.domain.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones del módulo {@code cuentas} a RFC 9457
 * {@link ProblemDetail} con un código estable.
 *
 * <p>El alcance se limita al controlador de cuentas para no interferir con el
 * manejo por defecto de otros módulos. Los detalles internos y las trazas nunca
 * se exponen en la respuesta.</p>
 */
@RestControllerAdvice(basePackageClasses = CuentaFinancieraController.class)
public class CuentaFinancieraExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(CuentaFinancieraExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleIdentificadorInvalido(MethodArgumentTypeMismatchException ex) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "ID_INVALIDO",
            "El parámetro '" + ex.getName() + "' no tiene un formato válido.");
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNoEncontrado(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(DatosPersistidosInvalidos.class)
    public ProblemDetail handleDatosInconsistentes(DatosPersistidosInvalidos ex) {
        log.error("Datos persistidos inconsistentes al consultar cuentas", ex);
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
        if (ex instanceof ErrorResponse errorResponse) {
            ProblemDetail body = errorResponse.getBody();
            if (body.getProperties() == null || !body.getProperties().containsKey("code")) {
                body.setProperty("code", codigoPara(errorResponse.getStatusCode()));
            }
            return body;
        }

        log.error("Error no controlado al consultar cuentas", ex);
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
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 422 -> "Regla de negocio incumplida";
            case 500 -> "Error interno";
            default -> "Error HTTP";
        };
    }

    private static String codigoPara(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "VALIDACION_ENTRADA";
            case 404 -> "NO_ENCONTRADO";
            case 405 -> "METODO_NO_PERMITIDO";
            case 406 -> "TIPO_RESPUESTA_NO_SOPORTADO";
            case 415 -> "TIPO_CONTENIDO_NO_SOPORTADO";
            case 500 -> "ERROR_INTERNO";
            default -> "ERROR_HTTP";
        };
    }
}
