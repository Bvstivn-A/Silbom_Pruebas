package cl.silbom.backend.service;

import cl.silbom.backend.dto.MaquinaCreateDTO;
import cl.silbom.backend.dto.MaquinaResponseDTO;
import cl.silbom.backend.dto.MaquinaUpdateDTO;
import cl.silbom.backend.exception.BotDetectionException;
import cl.silbom.backend.exception.BusinessConflictException;
import cl.silbom.backend.exception.ResourceAlreadyExistsException;
import cl.silbom.backend.exception.ResourceNotFoundException;
import cl.silbom.backend.model.EstadoBloqueo;
import cl.silbom.backend.model.Maquina;
import cl.silbom.backend.repository.BloqueoLotoRepository;
import cl.silbom.backend.repository.MaquinaRepository;
import cl.silbom.backend.security.InputSanitizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * QUÉ HACE:
 * Servicio de lógica de negocio para la gestión de maquinaria en Planta San Joaquín.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - CONTROL 7 (Anti-IDOR): Todas las operaciones validan la pertenencia a la planta autorizada.
 * - CONTROL 12 (Anti-Bots): Descarta peticiones que completen campos señuelo (honeypot).
 * - CONTROL 15 (Sanitización): Limpia etiquetas HTML de textos libres antes de persistir.
 * - Maneja atomicidad y consistencia de datos mediante @Transactional.
 */
@Service
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final BloqueoLotoRepository bloqueoLotoRepository;
    private final String plantaId;
    private final cl.silbom.backend.security.ActorActual actor;

    public MaquinaService(MaquinaRepository maquinaRepository,
                          BloqueoLotoRepository bloqueoLotoRepository,
                          @Value("${silbom.security.planta-id:PLANTA-SAN-JOAQUIN}") String plantaId, cl.silbom.backend.security.ActorActual actor) {
        this.maquinaRepository = maquinaRepository;
        this.bloqueoLotoRepository = bloqueoLotoRepository;
        this.plantaId = plantaId;
        this.actor = actor;
    }

    /**
     * Lista todas las máquinas de la planta con filtro opcional de bloqueo.
     */
    @Transactional(readOnly = true)
    public List<MaquinaResponseDTO> listarMaquinas(Boolean bloqueada) {
        return maquinaRepository.findMaquinasFiltradas(this.plantaId, bloqueada)
            .stream()
            .map(MaquinaResponseDTO::fromEntity)
            .toList();
    }

    /**
     * Obtiene una máquina por su identificador único dentro de la planta autorizada.
     */
    @Transactional(readOnly = true)
    public MaquinaResponseDTO obtenerPorId(String id) {
        Maquina maquina = maquinaRepository.findByIdAndPlantaIdAndActivaTrue(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina con ID '" + id + "' no encontrada en " + this.plantaId));
        return MaquinaResponseDTO.fromEntity(maquina);
    }

    /**
     * Da de alta una nueva máquina previa validación y sanitización.
     */
    @Transactional
    public MaquinaResponseDTO crearMaquina(MaquinaCreateDTO dto) {
        actor.exigirSupervisor();
        // Control 12: Detección de bots por trampa honeypot
        if (dto.honeypot() != null && !dto.honeypot().isBlank()) {
            throw new BotDetectionException("Actividad automatizada detectada vía honeypot.");
        }

        // Validación de existencia previa
        if (maquinaRepository.existsByIdAndPlantaId(dto.id(), this.plantaId)) {
            throw new ResourceAlreadyExistsException("La máquina con ID '" + dto.id() + "' ya existe en el sistema.");
        }

        // Control 15: Sanitización de entradas de texto libre
        String nombreSanitizado = InputSanitizer.required(dto.nombre(),3,100);
        String areaSanitizada = InputSanitizer.required(dto.area(),3,100);
        String iconoSanitizado = InputSanitizer.required(dto.icono(),1,10);

        Maquina nuevaMaquina = new Maquina(
            dto.id(),
            nombreSanitizado,
            areaSanitizada,
            iconoSanitizado,
            dto.horasOperativas(),
            dto.fallasCorrectivas(),
            this.plantaId
        );

        Maquina guardada = maquinaRepository.save(nuevaMaquina);
        return MaquinaResponseDTO.fromEntity(guardada);
    }

    /**
     * Actualiza atributos operativos de la máquina y recalcula su MTBF.
     */
    @Transactional
    public MaquinaResponseDTO actualizarMaquina(String id, MaquinaUpdateDTO dto) {
        actor.exigirSupervisor();
        Maquina maquina = maquinaRepository.findForUpdate(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina con ID '" + id + "' no encontrada para actualizar"));

        String nombreSanitizado = InputSanitizer.required(dto.nombre(),3,100);
        String areaSanitizada = InputSanitizer.required(dto.area(),3,100);
        String iconoSanitizado = InputSanitizer.required(dto.icono(),1,10);

        maquina.actualizarDatos(
            nombreSanitizado,
            areaSanitizada,
            iconoSanitizado,
            dto.horasOperativas(),
            dto.fallasCorrectivas()
        );

        Maquina actualizada = maquinaRepository.save(maquina);
        return MaquinaResponseDTO.fromEntity(actualizada);
    }

    /**
     * Elimina una máquina del catálogo si no está bloqueada ni tiene bloqueos activos.
     */
    @Transactional
    public void eliminarMaquina(String id) {
        actor.exigirSupervisor();
        Maquina maquina = maquinaRepository.findForUpdate(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina con ID '" + id + "' no encontrada para eliminar"));

        // Regla de Seguridad Industrial LOTO: Prohibido eliminar equipos con personal trabajando
        if (Boolean.TRUE.equals(maquina.getBloqueada()) || (maquina.getBloqueosActivos() != null && maquina.getBloqueosActivos() > 0)) {
            throw new BusinessConflictException("No se puede eliminar la máquina '" + id + "' porque actualmente se encuentra BLOQUEADA ("
                + maquina.getBloqueosActivos() + " técnicos interviniendo)");
        }

        long bloqueosActivos = bloqueoLotoRepository.countByMaquinaIdAndEstado(id, EstadoBloqueo.ACTIVO);
        if (bloqueosActivos > 0) {
            throw new BusinessConflictException("No se puede eliminar la máquina '" + id + "' porque tiene " + bloqueosActivos + " bloqueos LOTO activos");
        }

        maquina.desactivar();
    }
}
