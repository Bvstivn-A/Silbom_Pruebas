package cl.silbom.backend.exception;

/**
 * QUÉ HACE:
 * Excepción lanzada cuando se detecta actividad automatizada de bot (honeypot o User-Agent malicioso).
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Retorna el código HTTP 400 Bad Request deteniendo el ataque inmediatamente.
 */
public class BotDetectionException extends RuntimeException {
    public BotDetectionException(String message) {
        super(message);
    }
}
