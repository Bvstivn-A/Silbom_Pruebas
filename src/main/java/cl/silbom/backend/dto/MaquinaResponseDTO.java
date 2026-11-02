package cl.silbom.backend.dto;

import cl.silbom.backend.model.Maquina;

/**
 * QUÉ HACE:
 * DTO inmutable de salida con los datos de una máquina industrial.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 17 (Devolver solo los datos necesarios): Nunca devuelve directamente la entidad
 *   JPA cruda, evitando fuga de detalles de infraestructura o persistencia.
 */
public record MaquinaResponseDTO(
    String id,
    String nombre,
    String area,
    String icono,
    Double horasOperativas,
    Integer fallasCorrectivas,
    Double mtbf,
    Boolean bloqueada,
    Integer bloqueosActivos,
    String plantaId
) {
    /**
     * Mapper estático para transformar de forma segura una entidad Maquina a su DTO de salida.
     */
    public static MaquinaResponseDTO fromEntity(Maquina m) {
        return new MaquinaResponseDTO(
            m.getId(),
            m.getNombre(),
            m.getArea(),
            m.getIcono(),
            m.getHorasOperativas(),
            m.getFallasCorrectivas(),
            m.getMtbf(),
            m.getBloqueada(),
            m.getBloqueosActivos(),
            m.getPlantaId()
        );
    }
}
