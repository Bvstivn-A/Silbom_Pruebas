package cl.silbom.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Control 18: Cabeceras defensivas HTTP
        String uri = request.getRequestURI();
        boolean esVistaWeb =
                "/".equals(uri)
                        || "/index.html".equals(uri)
                        || "/Silbom1.html".equals(uri)
                        || "/silbom-tecnico.html".equals(uri)
                        || "/app".equals(uri)
                        || "/app/".equals(uri)
                        || (uri != null && (uri.startsWith("/app/") || uri.startsWith("/docs/")));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader(
                "Content-Security-Policy",
                esVistaWeb
                        ? "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self'"
                            + " 'unsafe-inline'; img-src 'self' data:; connect-src 'self' http:"
                            + " https:"
                        : "default-src 'self'");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // HSTS solo en una conexión que ya es segura; no configura TLS.
        if (request.isSecure())
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        filterChain.doFilter(request, response);
    }
}
