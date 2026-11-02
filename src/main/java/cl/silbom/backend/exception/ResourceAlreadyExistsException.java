package cl.silbom.backend.exception;

/**
 * QUÉ HACE:
 * Excepción lanzada al intentar registrar un recurso cuyo identificador ya existe (ej: ID duplicado).
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Se mapea directamente al código HTTP 409 Conflict.
 */
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
