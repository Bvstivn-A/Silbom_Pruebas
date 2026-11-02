package cl.silbom.backend.dto;
import cl.silbom.backend.model.EventoLoto;
import java.time.LocalDateTime;
public record EventoLotoResponseDTO(Long id, Long bloqueoId, Long tecnicoId, String accion, LocalDateTime fecha) {
    public static EventoLotoResponseDTO from(EventoLoto e) {
        return new EventoLotoResponseDTO(e.getId(), e.getBloqueoId(), e.getTecnicoId(), e.getAccion(), e.getFecha());
    }
}
