package cl.silbom.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * QUÉ HACE:
 * Inyecta cabeceras HTTP de seguridad estandarizadas en todas las respuestas del servidor.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 18 (Cabeceras de seguridad):
 *   * X-Content-Type-Options: nosniff -> Evita que el navegador interprete archivos como MIME alternativo (MIME Sniffing).
 *   * X-Frame-Options: DENY -> Protege contra ataques de Clickjacking impidiendo incrustar la app en iframes.
 *   * Content-Security-Policy: default-src 'self' -> Restringe la carga de recursos únicamente al origen autorizado.
 *   * Referrer-Policy: strict-origin-when-cross-origin -> Evita fugar rutas y tokens en cabeceras Referer.
 * - CONTROL 19 (Forzar HTTPS):
 *   * Strict-Transport-Security (HSTS) -> Exige al navegador comunicarse exclusivamente mediante TLS/HTTPS durante 1 año.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Control 18: Cabeceras defensivas HTTP
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy", "default-src 'self'");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Control 19: HSTS (Strict-Transport-Security)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        filterChain.doFilter(request, response);
    }
}
