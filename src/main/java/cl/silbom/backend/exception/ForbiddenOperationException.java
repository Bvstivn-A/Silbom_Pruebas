package cl.silbom.backend.exception;

/**
 * QUÉ HACE:
 * Excepción lanzada cuando una petición intenta violar el perímetro de seguridad (Anti-IDOR).
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Retorna el código HTTP 403 Forbidden cuando el recurso pertenece a otra planta.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
