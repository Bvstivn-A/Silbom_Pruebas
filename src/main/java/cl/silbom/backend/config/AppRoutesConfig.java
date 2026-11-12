package cl.silbom.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * QUÉ HACE: Mapea /app y /app/ hacia la aplicación móvil estática de técnicos.
 *
 * <p>POR QUÉ ESTÁ ESCRITO ASÍ: Spring Boot resuelve automáticamente / hacia index.html, pero las
 * rutas de subdirectorio como /app/ requieren reenvío explícito para servir /app/index.html.
 */
@Configuration
public class AppRoutesConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/app").setViewName("forward:/app/index.html");
        registry.addViewController("/app/").setViewName("forward:/app/index.html");
    }
}
