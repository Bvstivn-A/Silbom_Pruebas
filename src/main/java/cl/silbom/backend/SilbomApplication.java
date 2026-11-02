package cl.silbom.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * QUÉ HACE:
 * Punto de entrada principal (main) para el servidor backend SILBOM en Spring Boot 3.3.x.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - Arranca el contenedor de inversión de control (IoC), la configuración automática y el servidor web embebido.
 * - Despliega el monolito en 3 capas (Controller -> Service -> Repository) asegurando la auditoría LOTO.
 */
@SpringBootApplication
public class SilbomApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilbomApplication.class, args);
    }
}
