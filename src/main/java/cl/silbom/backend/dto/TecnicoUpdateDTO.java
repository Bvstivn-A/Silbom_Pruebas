package cl.silbom.backend.dto;
import cl.silbom.backend.model.Rol;
import jakarta.validation.constraints.*;
public record TecnicoUpdateDTO(
    @NotBlank @Size(min=3,max=80) String nombre,
    @NotNull Rol rol,
    @Size(min=12,max=72) String password
) { @Override public String toString() { return "TecnicoUpdateDTO[redacted]"; } }
