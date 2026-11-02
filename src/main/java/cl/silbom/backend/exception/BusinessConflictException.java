package cl.silbom.backend.exception;

/**
 * QUÉ HACE:
 * Excepción lanzada cuando una operación viola una regla de seguridad o del dominio industrial:
 * - Intentar eliminar una máquina que se encuentra bloqueada o con intervenciones activas.
 * - Intentar eliminar un bloqueo LOTO que se encuentra ACTIVO.
 * - Intentar liberar un bloqueo LOTO que ya había sido CERRADO.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Se mapea directamente al código HTTP 409 Conflict.
 */
public class BusinessConflictException extends RuntimeException {
    public BusinessConflictException(String message) {
        super(message);
    }
}
