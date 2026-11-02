package cl.silbom.backend.dto;
import cl.silbom.backend.model.Rol;
import jakarta.validation.constraints.*;
public record TecnicoCreateDTO(
    @NotBlank @Pattern(regexp="[a-zA-Z0-9._-]{3,50}") String username,
    @NotBlank @Size(min=3,max=80) String nombre,
    @NotBlank @Size(min=12,max=72) String password,
    @NotNull Rol rol
) { @Override public String toString() { return "TecnicoCreateDTO[" + username + "]"; } }
