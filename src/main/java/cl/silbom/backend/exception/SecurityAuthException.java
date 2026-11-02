package cl.silbom.backend.exception;

/**
 * QUÉ HACE:
 * Excepción lanzada cuando la autenticación por API Key falla o está ausente en rutas protegidas.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Retorna el código HTTP 401 Unauthorized.
 */
public class SecurityAuthException extends RuntimeException {
    public SecurityAuthException(String message) {
        super(message);
    }
}
