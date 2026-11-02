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
import cl.silbom.backend.security.ActorActual;
import cl.silbom.backend.model.*;
import cl.silbom.backend.repository.*;
import cl.silbom.backend.dto.EventoLotoResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BloqueoService {

    private final BloqueoLotoRepository bloqueoLotoRepository;
    private final MaquinaRepository maquinaRepository;
    private final CryptoUtils cryptoUtils;
    private final String plantaId;
    private final ActorActual actor;
    private final TecnicoRepository tecnicos;
    private final FallaRepository fallas;
    private final EventoLotoRepository eventos;

    public BloqueoService(BloqueoLotoRepository bloqueoLotoRepository,
                          MaquinaRepository maquinaRepository,
                          CryptoUtils cryptoUtils,
                          @Value("${silbom.security.planta-id:PLANTA-SAN-JOAQUIN}") String plantaId,
                          ActorActual actor, TecnicoRepository tecnicos, FallaRepository fallas, EventoLotoRepository eventos) {
        this.bloqueoLotoRepository = bloqueoLotoRepository;
        this.maquinaRepository = maquinaRepository;
        this.cryptoUtils = cryptoUtils;
        this.plantaId = plantaId;
        this.actor = actor; this.tecnicos = tecnicos; this.fallas = fallas; this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public List<BloqueoResponseDTO> listarBloqueos(String maquinaId, EstadoBloqueo estado, boolean incluirNotas) {
        actor.obtener();
        return bloqueoLotoRepository.findBloqueosFiltrados(this.plantaId, maquinaId, estado)
            .stream()
            .map(b -> BloqueoResponseDTO.fromEntity(b, notasVisibles(b, incluirNotas)))
            .toList();
    }

    @Transactional(readOnly = true)
    public BloqueoResponseDTO obtenerPorId(Long id, boolean incluirNotas) {
        BloqueoLoto bloqueo = bloqueoLotoRepository.findByIdAndPlantaId(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO #" + id + " no encontrado"));
        return BloqueoResponseDTO.fromEntity(bloqueo, notasVisibles(bloqueo, incluirNotas));
    }

    private String notasVisibles(BloqueoLoto bloqueo, boolean incluirNotas) {
        var actual = actor.obtener();
        boolean autorizado = actual.rol() == Rol.SUPERVISOR || actual.id().equals(bloqueo.getTecnicoId());
        return incluirNotas && autorizado ? cryptoUtils.decrypt(bloqueo.getNotasAuditoriaCifradas()) : null;
    }

    @Transactional
    public BloqueoResponseDTO crearBloqueo(BloqueoCreateDTO dto) {
        var responsable = tecnicos.findForUpdate(actor.obtener().id(), plantaId)
            .orElseThrow(() -> new cl.silbom.backend.exception.ForbiddenOperationException("Técnico inactivo o de otra planta"));
        if (!dto.isReferenciaCoherente()) throw new IllegalArgumentException("CORRECTIVA requiere referenciaFalla; PREVENTIVA no la admite");
        if (dto.notasConfidenciales()!=null && dto.notasConfidenciales().length()>2000)
            throw new IllegalArgumentException("Notas demasiado extensas");
        if (dto.honeypot() != null && !dto.honeypot().isBlank()) {
            throw new BotDetectionException("Actividad automatizada detectada vía honeypot.");
        }

        if (dto.confirmacionCandadoDs594() == null || !dto.confirmacionCandadoDs594()) {
            throw new IllegalArgumentException("Debe confirmar haber colocado el candado físico conforme al DS 594.");
        }

        Maquina maquina = maquinaRepository.findForUpdate(dto.maquinaId(), this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina '" + dto.maquinaId() + "' no encontrada"));

        String motivoSanitizado = InputSanitizer.required(dto.motivo(),5,250);
        String tecnicoSanitizado = responsable.getNombre();
        boolean nuevaFalla = false;
        Long fallaId = null;
        if (dto.tipoIntervencion() == TipoIntervencion.CORRECTIVA) {
            if (!dto.referenciaFalla().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))
                throw new IllegalArgumentException("Referencia de falla inválida");
            var existente = fallas.findByMaquinaIdAndPlantaIdAndReferencia(maquina.getId(), plantaId, dto.referenciaFalla());
            nuevaFalla = existente.isEmpty();
            fallaId = existente.orElseGet(() -> fallas.save(new Falla(maquina.getId(), plantaId, dto.referenciaFalla()))).getId();
        }

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

        nuevoBloqueo.asignarResponsable(responsable.getId(), fallaId);

        // Aplicar regla de dominio sobre la máquina (Multi-LOTO y MTBF)
        maquina.registrarEntradaBloqueo(nuevaFalla);

        maquinaRepository.save(maquina);
        BloqueoLoto guardado = bloqueoLotoRepository.save(nuevoBloqueo);

        eventos.save(new EventoLoto(guardado.getId(), responsable.getId(), "CREADO", plantaId));
        return BloqueoResponseDTO.fromEntity(guardado, dto.notasConfidenciales());
    }

    @Transactional
    public BloqueoResponseDTO liberarBloqueo(Long id) {
        var responsable = tecnicos.findForUpdate(actor.obtener().id(), plantaId)
            .orElseThrow(() -> new cl.silbom.backend.exception.ForbiddenOperationException("Técnico inactivo"));
        String maquinaId = bloqueoLotoRepository.findMaquinaId(id, plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO no encontrado"));
        // Siempre máquina antes de ticket: evita contadores perdidos y cierres dobles.
        Maquina maquina = maquinaRepository.findForUpdate(maquinaId, plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Máquina no encontrada"));
        BloqueoLoto bloqueo = bloqueoLotoRepository.findForUpdate(id, plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO no encontrado"));
        actor.exigirPropietario(bloqueo.getTecnicoId());
        if (bloqueo.getEstado() == EstadoBloqueo.CERRADO)
            throw new BusinessConflictException("El ticket LOTO ya fue liberado");
        bloqueo.cerrarTicket();

        maquina.registrarSalidaBloqueo();

        maquinaRepository.save(maquina);
        BloqueoLoto actualizado = bloqueoLotoRepository.save(bloqueo);

        eventos.save(new EventoLoto(id, responsable.getId(), "LIBERADO", plantaId));
        return BloqueoResponseDTO.fromEntity(actualizado, cryptoUtils.decrypt(actualizado.getNotasAuditoriaCifradas()));
    }

    @Transactional
    public void eliminarBloqueo(Long id) {
        BloqueoLoto bloqueo = bloqueoLotoRepository.findByIdAndPlantaId(id, this.plantaId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket LOTO #" + id + " no encontrado para eliminar"));

        throw new BusinessConflictException("El historial LOTO es inmutable: no se eliminan tickets activos ni CERRADOS");
    }

    @Transactional(readOnly = true)
    public List<EventoLotoResponseDTO> eventos(Long id) {
        obtenerPorId(id, false);
        return eventos.findByBloqueoIdAndPlantaIdOrderById(id, plantaId).stream().map(EventoLotoResponseDTO::from).toList();
    }
}
