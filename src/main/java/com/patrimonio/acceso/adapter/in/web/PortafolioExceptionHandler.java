package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.application.error.MonedaInvalidaException;
import com.patrimonio.acceso.application.error.PortafolioInvalidoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores de negocio de portafolios a {@link ProblemDetail}.
 *
 * <p>El alcance se limita a los controladores de portafolios y monedas. La
 * validación del contrato de entrada y los errores inesperados siguen
 * gestionándose en el manejador de autenticación, que comparte el paquete.
 * Esta clase tiene prioridad para que sus códigos específicos no se conviertan
 * en un error genérico.</p>
 */
@RestControllerAdvice(assignableTypes = {
    PortafolioController.class,
    MonedaController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PortafolioExceptionHandler {

    @ExceptionHandler(PortafolioInvalidoException.class)
    public ProblemDetail handlePortafolioInvalido(
        PortafolioInvalidoException ex
    ) {
        return problem(HttpStatus.BAD_REQUEST, ex.codigo(), ex.getMessage());
    }

    @ExceptionHandler(MonedaInvalidaException.class)
    public ProblemDetail handleMonedaInvalida(MonedaInvalidaException ex) {
        return problem(
            HttpStatus.UNPROCESSABLE_CONTENT,
            ex.codigo(),
            ex.getMessage());
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
            case 422 -> "Regla de negocio incumplida";
            default -> "Error HTTP";
        };
    }
}
