package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.adapter.in.security.SesionExpiracionFilter;
import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.AutenticarUsuarioCommand;
import com.patrimonio.acceso.application.port.in.AutenticarUsuarioUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Adaptador de entrada para autenticación e identidad.
 *
 * <p>Verifica las credenciales mediante un caso de uso y crea la sesión. La
 * sesión se guarda de forma explícita porque la cadena de seguridad no usa el
 * login por formulario. El token CSRF se rota al autenticar y se elimina al
 * cerrar sesión.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String MECANISMO = "SESSION";

    private final AutenticarUsuarioUseCase autenticarUsuario;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final Duration tiempoInactividad;
    private final Duration duracionMaxima;

    public AuthController(
        AutenticarUsuarioUseCase autenticarUsuario,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository,
        @Value("${server.servlet.session.timeout:30m}") Duration tiempoInactividad,
        @Value("${patrimonio.seguridad.sesion.duracion-maxima:8h}")
        Duration duracionMaxima
    ) {
        this.autenticarUsuario = autenticarUsuario;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.tiempoInactividad = tiempoInactividad;
        this.duracionMaxima = duracionMaxima;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        UsuarioAutenticado usuario = autenticarUsuario.execute(
            new AutenticarUsuarioCommand(request.usuario(), request.password()));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            usuario,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))));
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        session.setAttribute(
            SesionExpiracionFilter.ATRIBUTO_INICIO_SESION,
            Instant.now());
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        CsrfToken csrf = csrfTokenRepository.generateToken(httpRequest);
        csrfTokenRepository.saveToken(csrf, httpRequest, httpResponse);

        return ResponseEntity.ok(new LoginResponse(
            MECANISMO,
            UsuarioResponse.from(usuario),
            tiempoInactividad.toSeconds(),
            duracionMaxima.toSeconds(),
            csrf.getToken()));
    }

    @GetMapping("/me")
    public UsuarioResponse me(
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return UsuarioResponse.from(usuario);
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest httpRequest) {
        CsrfToken token = (CsrfToken) httpRequest.getAttribute(
            CsrfToken.class.getName());
        return new CsrfResponse(
            token.getHeaderName(),
            token.getParameterName(),
            token.getToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        csrfTokenRepository.saveToken(null, httpRequest, httpResponse);
    }
}
