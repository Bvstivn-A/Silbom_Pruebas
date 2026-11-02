package cl.silbom.backend.dto;
import cl.silbom.backend.model.*;
public record TecnicoResponseDTO(Long id, String username, String nombre, Rol rol, boolean activo) {
    public static TecnicoResponseDTO from(Tecnico t) {
        return new TecnicoResponseDTO(t.getId(), t.getUsername(), t.getNombre(), t.getRol(), t.isActivo());
    }
}
