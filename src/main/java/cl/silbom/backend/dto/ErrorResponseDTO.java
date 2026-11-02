package cl.silbom.backend.dto;

import java.time.LocalDateTime;

/**
 * QUÉ HACE:
 * Estructura estándar y limpia para devolver errores HTTP en formato JSON.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 17 (Devolver solo los datos necesarios y ocultar stack traces):
 *   Garantiza que nunca se envíe al cliente trazas de la JVM, nombres de paquetes
 *   internos ni excepciones crudas, evitando entregar información valiosa a atacantes.
 */
public record ErrorResponseDTO(
    LocalDateTime timestamp,
    int status,
    String error,
    String mensaje,
    String ruta
) {}
