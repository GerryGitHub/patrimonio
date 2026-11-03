package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.application.error.CredencialesInvalidasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores de autenticación y del contrato de entrada a
 * {@link ProblemDetail}.
 *
 * <p>Los mensajes son fijos. No se devuelven valores enviados, correos,
 * contraseñas, hashes ni trazas.</p>
 */
@RestControllerAdvice(basePackageClasses = AuthController.class)
public class AuthExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail handleCredencialesInvalidas(
        CredencialesInvalidasException ex
    ) {
        return problem(
            HttpStatus.UNAUTHORIZED,
            "NO_AUTENTICADO",
            "Credenciales inválidas.");
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    public ProblemDetail handleSolicitudInvalida(Exception ex) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "VALIDACION_ENTRADA",
            "La solicitud no cumple el formato esperado.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErrorInesperado(Exception ex) {
        log.error("Error no controlado en autenticación", ex);
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
            case 401 -> "No autenticado";
            case 403 -> "Acceso denegado";
            case 500 -> "Error interno";
            default -> "Error HTTP";
        };
    }
}
