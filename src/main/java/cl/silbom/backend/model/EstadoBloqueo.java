package cl.silbom.backend.model;

/**
 * QUÉ HACE:
 * Representa el ciclo de vida de un candado digital LOTO.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Un ticket nace en estado ACTIVO y solo puede transicionar hacia CERRADO cuando
 * el técnico responsable retira su candado físico y digitaliza la salida.
 *
 * SEGURIDAD INDUSTRIAL:
 * Ningún ticket en estado ACTIVO puede ser eliminado físicamente de la base de datos
 * para evitar el borrado de evidencia de intervenciones en curso.
 */
public enum EstadoBloqueo {
    ACTIVO,
    CERRADO
}
