package cl.silbom.backend.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.List;

@Configuration @EnableWebSecurity
public class SecurityConfig {
    private final SecurityHeadersFilter headers;
    private final RateLimitFilter rate;
    public SecurityConfig(SecurityHeadersFilter headers, RateLimitFilter rate) { this.headers=headers; this.rate=rate; }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // API JSON usada por clientes explícitos. Antes de integrar login en navegador,
            // revisar CSRF: Basic puede ser reenviado automáticamente por el navegador.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/maquinas/**").permitAll()
                .requestMatchers("/api/v1/tecnicos/**").hasRole("SUPERVISOR")
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated())
            .httpBasic(basic -> basic.authenticationEntryPoint((request,response,exception) -> {
                response.setHeader("WWW-Authenticate", "Basic realm=\"SILBOM\"");
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":401,\"mensaje\":\"Credencial individual ausente o inválida\"}");
            }))
            .exceptionHandling(errors -> errors.accessDeniedHandler((request,response,exception) -> {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status\":403,\"mensaje\":\"Operación no autorizada\"}");
            }))
            .addFilterBefore(rate, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(headers, RateLimitFilter.class)
            .build();
    }
    @Bean public CorsConfigurationSource corsConfigurationSource() {
        var config=new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000","http://localhost:5173","http://127.0.0.1:5500","https://silbom.netlify.app"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","HEAD"));
        config.setAllowedHeaders(List.of("Authorization","Content-Type","Accept","Origin","User-Agent"));
        config.setAllowCredentials(true);config.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",config);return source;
    }
    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}
