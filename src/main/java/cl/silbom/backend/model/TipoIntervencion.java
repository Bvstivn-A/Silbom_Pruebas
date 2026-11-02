package cl.silbom.backend.model;

/**
 * QUÉ HACE:
 * Define los tipos de intervención técnica permitidos en un protocolo LOTO.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Se utiliza un Enum para restringir los valores estrictamente a CORRECTIVA o PREVENTIVA
 * tanto en la base de datos como en la capa REST, evitando cadenas arbitrarias.
 *
 * REGLA DE NEGOCIO MTBF:
 * - CORRECTIVA: Intervención por avería no planificada. Incrementa el contador 'fallasCorrectivas'
 *   de la máquina y penaliza el MTBF.
 * - PREVENTIVA: Mantención programada rutinaria. NO incrementa 'fallasCorrectivas' ni penaliza el MTBF.
 */
public enum TipoIntervencion {
    CORRECTIVA,
    PREVENTIVA
}
