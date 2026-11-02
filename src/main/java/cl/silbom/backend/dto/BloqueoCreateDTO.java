package cl.silbom.backend.dto;

import cl.silbom.backend.model.TipoIntervencion;
import jakarta.validation.constraints.*;

/**
 * QUÉ HACE:
 * DTO inmutable para la apertura de un bloqueo LOTO por parte de un técnico de planta.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 14 (Validación de entradas) y REGLA LEGAL DS 594:
 *   La anotación @AssertTrue sobre 'confirmacionCandadoDs594' bloquea en el gateway REST cualquier
 *   solicitud que no venga con 'true', garantizando el cumplimiento del marco legal industrial chileno.
 * - CONTROL 12 (Protección contra bots): Campo 'honeypot' para detectar scripts automatizados no autorizados.
 */
public record BloqueoCreateDTO(
    @NotBlank(message = "El identificador de la máquina es obligatorio")
    @Pattern(regexp = "^EQ-[A-Z]{2,4}-\\d{2}$", message = "El ID debe cumplir con el formato estándar EQ-XXX-00 (ej: EQ-ENV-01)")
    String maquinaId,

    @NotNull(message = "El tipo de intervención es obligatorio (CORRECTIVA o PREVENTIVA)")
    TipoIntervencion tipoIntervencion,

    @NotBlank(message = "El motivo de la intervención es obligatorio")
    @Size(min = 5, max = 250, message = "El motivo debe tener entre 5 y 250 caracteres")
    String motivo,

    @NotBlank(message = "El nombre del técnico responsable es obligatorio")
    @Size(min = 3, max = 80, message = "El nombre del técnico debe tener entre 3 y 80 caracteres")
    String nombreTecnico,

    @NotNull(message = "La confirmación del candado físico es obligatoria")
    @AssertTrue(message = "Por exigencia legal del DS 594 y DS 44, debe confirmar la colocación del candado físico mecánico antes de registrar el bloqueo digital")
    Boolean confirmacionCandadoDs594,

    // Notas de auditoría confidenciales opcionales que se cifrarán con AES-256-GCM
    String notasConfidenciales,

    // Campo trampa para detectar bots automatizados
    String honeypot
) {}
