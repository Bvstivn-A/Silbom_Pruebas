package cl.silbom.backend.exception;

/**
 * QUÉ HACE:
 * Excepción lanzada cuando una máquina o ticket LOTO no existe en la base de datos.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Permite que el GlobalExceptionHandler la capture y retorne exactamente el código HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
