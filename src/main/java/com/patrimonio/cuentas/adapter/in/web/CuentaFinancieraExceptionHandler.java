package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import com.patrimonio.shared.domain.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce las excepciones del módulo {@code cuentas} a RFC 9457
 * {@link ProblemDetail} con un código estable.
 *
 * <p>El alcance se limita al controlador de cuentas para no interferir con el
 * manejo por defecto de otros módulos. Los detalles internos, las trazas y los
 * valores financieros nunca se exponen en la respuesta; solo se registran en
 * el log del servidor. Los mensajes devueltos al cliente son fijos y no
 * incluyen el valor de los parámetros ni datos de la base.</p>
 */
@RestControllerAdvice(basePackageClasses = CuentaFinancieraController.class)
public class CuentaFinancieraExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(CuentaFinancieraExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleIdentificadorInvalido(MethodArgumentTypeMismatchException ex) {
        // Solo se usa el nombre del parámetro, nunca su valor.
        return problem(
            HttpStatus.BAD_REQUEST,
            "ID_INVALIDO",
            "El parámetro '" + ex.getName() + "' no tiene un formato válido.");
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

    @ExceptionHandler(DatosPersistidosInvalidos.class)
    public ProblemDetail handleDatosInconsistentes(DatosPersistidosInvalidos ex) {
        // La traza queda en el log; el cliente recibe un mensaje genérico.
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

    /**
     * Red de seguridad de último recurso.
     *
     * <p>No sustituye el manejo específico: cualquier fallo de programación se
     * registra con su traza completa para diagnóstico y el cliente recibe un
     * mensaje genérico sin detalles internos.</p>
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleErrorInesperado(Exception ex) {
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
            case 403 -> "Acceso denegado";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método no permitido";
            case 406 -> "Tipo de respuesta no soportado";
            case 415 -> "Tipo de contenido no soportado";
            case 422 -> "Regla de negocio incumplida";
            case 500 -> "Error interno";
            default -> "Error HTTP";
        };
    }
}
