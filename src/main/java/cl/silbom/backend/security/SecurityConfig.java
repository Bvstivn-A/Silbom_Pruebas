package cl.silbom.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * QUÉ HACE:
 * Configuración central del módulo Spring Security para la API de SILBOM.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 6 (Autenticación y Default Deny): Deshabilita sesiones de servidor por estado (STATELESS)
 *   e integra la cadena de filtros de ciberseguridad.
 * - CONTROL 10 (Hashear contraseñas): Expone el bean BCryptPasswordEncoder con factor de costo 12
 *   para garantizar que las credenciales de usuarios sean irreversibles y resistentes a fuerza bruta.
 * - CONTROL 18 (CORS Estricto): Define una lista blanca cerrada de orígenes autorizados
 *   (frontend local en Vite/React 5173, Next.js 3000, Live Server 5500 y despliegue Netlify),
 *   prohibiendo taxativamente el comodín abierto '*' en producción.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityHeadersFilter securityHeadersFilter;
    private final RateLimitFilter rateLimitFilter;
    private final ApiKeyAuthFilter apiKeyAuthFilter;

    public SecurityConfig(SecurityHeadersFilter securityHeadersFilter,
                          RateLimitFilter rateLimitFilter,
                          ApiKeyAuthFilter apiKeyAuthFilter) {
        this.securityHeadersFilter = securityHeadersFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.apiKeyAuthFilter = apiKeyAuthFilter;
    }

    /**
     * Define la cadena de filtros HTTP de seguridad de Spring Security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Configuración de CORS estricto
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 2. Deshabilitar CSRF dado que la API es Stateless y utiliza tokens/API Keys en cabeceras
            .csrf(AbstractHttpConfigurer::disable)

            // 3. Política de sesión sin estado (Stateless): no almacena cookies de sesión JSESSIONID en memoria
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 4. Autorización a nivel de servlet (las mutaciones son validadas en detalle por ApiKeyAuthFilter)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**").permitAll()
                .anyRequest().authenticated()
            )

            // 5. Inyección ordenada de filtros de ciberseguridad antes del filtro base de Spring
            .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, SecurityHeadersFilter.class)
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CONTROL 18: Política CORS con lista blanca de orígenes permitidos.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:5500",
            "https://silbom.netlify.app"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-API-KEY", "Accept", "Origin", "User-Agent"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache pre-flight durante 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * CONTROL 10: Algoritmo de hashing irreversible BCrypt con factor de costo 12.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
