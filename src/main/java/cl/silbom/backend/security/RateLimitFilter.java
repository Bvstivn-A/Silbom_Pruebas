package cl.silbom.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import cl.silbom.backend.dto.ErrorResponseDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QUÉ HACE:
 * Limita la tasa de peticiones por IP y filtra clientes sin User-Agent o con herramientas de escaneo comunes.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 11 (Rate Limiting): ventana FIJA de 60 s por IP y por tipo de operación:
 *   lecturas (GET/HEAD/OPTIONS) hasta 60/min y escrituras (POST/PUT/DELETE/...) hasta 20/min.
 *   Al excederse responde 429 con cabecera Retry-After.
 * - La IP se toma SIEMPRE de request.getRemoteAddr(). Nunca de X-Forwarded-For, porque cualquier
 *   cliente puede falsificar esa cabecera y saltarse el límite. Si algún día hay un proxy delante,
 *   se configura server.forward-headers-strategy=native en application.properties para que el
 *   contenedor resuelva la IP real solo desde proxies de confianza.
 * - Los contadores vencidos se limpian periódicamente para que el mapa no crezca sin límite.
 * - CONTROL 12 (Anti-bots básico): User-Agent obligatorio y bloqueo de herramientas conocidas.
 *   Es una barrera débil (el UA es falsificable): complementa, no reemplaza, al rate limit.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_READS_PER_WINDOW = 60;
    private static final int MAX_WRITES_PER_WINDOW = 20;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final Set<String> METODOS_LECTURA = Set.of("GET", "HEAD", "OPTIONS");
    private static final String[] USER_AGENTS_BLOQUEADOS = {"sqlmap", "nikto", "masscan"};

    private final ObjectMapper objectMapper;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMillis = new AtomicLong(System.currentTimeMillis());

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Contador de una ventana fija. */
    private static final class Counter {
        private long windowStartMillis;
        private int count;

        Counter(long now) {
            this.windowStartMillis = now;
        }

        /** @return 0 si la petición se permite; si no, los segundos que faltan para que se reinicie la ventana. */
        synchronized long tryAcquire(long now, int max) {
            if (now - windowStartMillis >= WINDOW_MILLIS) {
                windowStartMillis = now;
                count = 0;
            }
            if (count < max) {
                count++;
                return 0;
            }
            long restanteMs = WINDOW_MILLIS - (now - windowStartMillis);
            return Math.max(1, (restanteMs + 999) / 1000);
        }

        synchronized boolean expired(long now) {
            return now - windowStartMillis >= WINDOW_MILLIS;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Control 12: User-Agent
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            enviarError(response, request, HttpStatus.BAD_REQUEST, "Petición inválida",
                        "Falta el encabezado User-Agent.");
            return;
        }
        String ua = userAgent.toLowerCase();
        for (String bloqueado : USER_AGENTS_BLOQUEADOS) {
            if (ua.contains(bloqueado)) {
                enviarError(response, request, HttpStatus.FORBIDDEN, "Acceso denegado",
                            "Cliente no autorizado.");
                return;
            }
        }

        // Control 11: rate limit por IP y tipo de operación
        long now = System.currentTimeMillis();
        limpiarVencidos(now);

        boolean esLectura = METODOS_LECTURA.contains(request.getMethod().toUpperCase());
        int maximo = esLectura ? MAX_READS_PER_WINDOW : MAX_WRITES_PER_WINDOW;
        String clave = request.getRemoteAddr() + (esLectura ? "|R" : "|W");

        Counter counter = counters.computeIfAbsent(clave, k -> new Counter(now));
        long reintentarEn = counter.tryAcquire(now, maximo);

        if (reintentarEn > 0) {
            response.setHeader("Retry-After", String.valueOf(reintentarEn));
            enviarError(response, request, HttpStatus.TOO_MANY_REQUESTS, "Demasiadas peticiones",
                        "Ha superado el límite de peticiones. Intente nuevamente más tarde.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Elimina contadores vencidos como máximo una vez por ventana. */
    private void limpiarVencidos(long now) {
        long ultima = lastCleanupMillis.get();
        if (now - ultima >= WINDOW_MILLIS && lastCleanupMillis.compareAndSet(ultima, now)) {
            counters.entrySet().removeIf(e -> e.getValue().expired(now));
        }
    }

    private void enviarError(HttpServletResponse response, HttpServletRequest request,
                             HttpStatus status, String error, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDTO dto = new ErrorResponseDTO(
            LocalDateTime.now(), status.value(), error, mensaje, request.getRequestURI());
        response.getWriter().write(objectMapper.writeValueAsString(dto));
    }
}
