package cl.silbom.backend.service;

import cl.silbom.backend.dto.BloqueoCreateDTO;
import cl.silbom.backend.dto.BloqueoResponseDTO;
import cl.silbom.backend.exception.BotDetectionException;
import cl.silbom.backend.exception.BusinessConflictException;
import cl.silbom.backend.exception.ResourceNotFoundException;
import cl.silbom.backend.model.BloqueoLoto;
import cl.silbom.backend.model.EstadoBloqueo;
import cl.silbom.backend.model.Maquina;
import cl.silbom.backend.repository.BloqueoLotoRepository;
import cl.silbom.backend.repository.MaquinaRepository;
import cl.silbom.backend.security.CryptoUtils;
import cl.silbom.backend.security.InputSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * QUÉ HACE:
 * Servicio de lógica de negocio para la gestión del protocolo LOTO (Lockout/Tagout).
 * Implementa las reglas críticas de desenergización en Energía Cero, Multi-LOTO,
 * validación legal de candados físicos (DS 594) y recálculo dinámico de MTBF.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - Toda operación de mutación es atómica (@Transactional): asegura que si falla la actualización
 *   de la máquina, el ticket de bloqueo no se persista a medias (integridad transaccional ACID).
 * - Multi-LOTO: Si hay múltiples técnicos sobre una máquina, esta sigue 'bloqueada = true'
 *   hasta que el último técnico retire su ticket (bloqueosActivos == 0).
 * - Control 5 (Cifrado): Encripta notas confidenciales con AES-256-GCM.
 */
@Service
public class BloqueoService {

    private final BloqueoLotoRepository bloqueoLotoRepository;
    private final MaquinaRepository maquinaRepository;
    private final CryptoUtils cryptoUtils;
    private final String plantaId;

    public BloqueoService(BloqueoLotoRepository bloqueoLotoRepository,
                          MaquinaRepository maquinaRepository,
                          CryptoUtils cryptoUtils,
                          @Value("${silbom.security.planta-id:PLANTA-SAN-JOAQUIN}") String plantaId) {
        this.bloqueoLotoRepository = bloqueoLotoRepository;
        this.maquinaRepository = maquinaRepository;
        this.cryptoUtils = cryptoUtils;
        this.plantaId = plantaId;
    }

    /**
     * Lista tickets de auditoría con filtros opcionales de máquina y estado,
     * ordenados del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public List<BloqueoResponseDTO> listarBloqueos(String maquinaId, EstadoBloqueo estado, boolean incluirNotas) {
        return bloqueoLotoRepository.findBloqueosFiltrados(this.plantaId, maquinaId, estado)
            .stream()
            .map(b -> BloqueoResponseDTO.fromEntity(b, notasVisibles(b, incluirNotas)))
            .toList();
    }

    /**
     * Obtiene un ticket por su ID dentro de la planta autorizada.
     */
    @Transactional(readOnly = true)
    public BloqueoResponseDTO obtenerPorId(Long id, boolean incluirNotas) {
        BloqueoLoto bloqueo = bloqueoLotoRepository.findByIdAndPlantaId(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO #" + id + " no encontrado"));
        return BloqueoResponseDTO.fromEntity(bloqueo, notasVisibles(bloqueo, incluirNotas));
    }

    /**
     * Las notas confidenciales solo se descifran y entregan si la petición vino con API key válida.
     * En las lecturas públicas (sin clave) el campo viaja como null.
     */
    private String notasVisibles(BloqueoLoto bloqueo, boolean incluirNotas) {
        return incluirNotas ? cryptoUtils.decrypt(bloqueo.getNotasAuditoriaCifradas()) : null;
    }

    /**
     * QUÉ HACE:
     * Registra un nuevo bloqueo LOTO sobre una máquina.
     *
     * REGLAS DE NEGOCIO Y CIBERSEGURIDAD:
     * 1. Control 12: Bloquea bots mediante el campo 'honeypot'.
     * 2. Ley DS 594: Obliga a que confirmacionCandadoDs594 sea true.
     * 3. Control 15: Sanitiza motivo y nombreTecnico para prevenir inyección XSS.
     * 4. Multi-LOTO: Incrementa 'bloqueosActivos' de la máquina y marca 'bloqueada = true'.
     * 5. MTBF: Si la intervención es CORRECTIVA, suma +1 a fallasCorrectivas y recalcula el MTBF.
     *    Si es PREVENTIVA, no altera las fallas ni penaliza el MTBF.
     * 6. Control 5: Cifra con AES-256-GCM las notas confidenciales de auditoría.
     */
    @Transactional
    public BloqueoResponseDTO crearBloqueo(BloqueoCreateDTO dto) {
        if (dto.honeypot() != null && !dto.honeypot().isBlank()) {
            throw new BotDetectionException("Actividad automatizada detectada vía honeypot.");
        }

        if (dto.confirmacionCandadoDs594() == null || !dto.confirmacionCandadoDs594()) {
            throw new IllegalArgumentException("Debe confirmar haber colocado el candado físico conforme al DS 594.");
        }

        Maquina maquina = maquinaRepository.findByIdAndPlantaId(dto.maquinaId(), this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina '" + dto.maquinaId() + "' no encontrada"));

        String motivoSanitizado = InputSanitizer.sanitize(dto.motivo());
        String tecnicoSanitizado = InputSanitizer.sanitize(dto.nombreTecnico());

        BloqueoLoto nuevoBloqueo = new BloqueoLoto(
            maquina.getId(),
            dto.tipoIntervencion(),
            motivoSanitizado,
            tecnicoSanitizado,
            true,
            this.plantaId
        );

        if (dto.notasConfidenciales() != null && !dto.notasConfidenciales().isBlank()) {
            nuevoBloqueo.setNotasAuditoriaCifradas(cryptoUtils.encrypt(dto.notasConfidenciales()));
        }

        // Aplicar regla de dominio sobre la máquina (Multi-LOTO y MTBF)
        maquina.registrarEntradaBloqueo(dto.tipoIntervencion());

        maquinaRepository.save(maquina);
        BloqueoLoto guardado = bloqueoLotoRepository.save(nuevoBloqueo);

        return BloqueoResponseDTO.fromEntity(guardado, dto.notasConfidenciales());
    }

    /**
     * QUÉ HACE:
     * Libera un bloqueo LOTO en curso (PUT /api/v1/bloqueos/{id}/liberar).
     *
     * REGLAS DE NEGOCIO Y CIBERSEGURIDAD:
     * - Si el ticket ya estaba CERRADO, rechaza la operación con 409 Conflict.
     * - Cierra el ticket, marca horaFin y calcula duracionMinutos.
     * - Regla Multi-LOTO: Resta 1 a bloqueosActivos de la máquina.
     *   SOLO si bloqueosActivos llega exactamente a 0, cambia 'bloqueada' a false.
     *   Si aún quedan otros técnicos (bloqueosActivos > 0), sigue 'bloqueada = true'.
     */
    @Transactional
    public BloqueoResponseDTO liberarBloqueo(Long id) {
        BloqueoLoto bloqueo = bloqueoLotoRepository.findByIdAndPlantaId(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO #" + id + " no encontrado para liberar"));

        if (bloqueo.getEstado() == EstadoBloqueo.CERRADO) {
            throw new BusinessConflictException("El ticket LOTO #" + id + " ya fue liberado previamente (Estado CERRADO)");
        }

        bloqueo.cerrarTicket();

        Maquina maquina = maquinaRepository.findByIdAndPlantaId(bloqueo.getMaquinaId(), this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina asociada '" + bloqueo.getMaquinaId() + "' no encontrada"));

        maquina.registrarSalidaBloqueo();

        maquinaRepository.save(maquina);
        BloqueoLoto actualizado = bloqueoLotoRepository.save(bloqueo);

        return BloqueoResponseDTO.fromEntity(actualizado, cryptoUtils.decrypt(actualizado.getNotasAuditoriaCifradas()));
    }

    /**
     * QUÉ HACE:
     * Elimina un registro histórico de bloqueo (DELETE /api/v1/bloqueos/{id}).
     *
     * REGLA DE SEGURIDAD INDUSTRIAL:
     * - Si el ticket está ACTIVO, prohíbe terminantemente su eliminación y devuelve 409 Conflict.
     *   Ningún candado digital activo puede borrarse mientras la máquina esté intervenida.
     * - Si el ticket está CERRADO, se permite el borrado devolviendo 204 No Content.
     */
    @Transactional
    public void eliminarBloqueo(Long id) {
        BloqueoLoto bloqueo = bloqueoLotoRepository.findByIdAndPlantaId(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO #" + id + " no encontrado para eliminar"));

        if (bloqueo.getEstado() == EstadoBloqueo.ACTIVO) {
            throw new BusinessConflictException("Violación de Seguridad Industrial: No se puede eliminar el ticket #"
                + id + " porque se encuentra ACTIVO. Debe liberarlo antes de archivar.");
        }

        bloqueoLotoRepository.delete(bloqueo);
    }
}
