package cl.silbom.backend.security;

import cl.silbom.backend.model.Rol;
import cl.silbom.backend.model.Tecnico;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public record TecnicoPrincipal(Long id, String username, String password, Rol rol, String plantaId, boolean activo)
        implements UserDetails {
    public TecnicoPrincipal(Tecnico t) { this(t.getId(), t.getUsername(), t.getPasswordHash(), t.getRol(), t.getPlantaId(), t.isActivo()); }
    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return password; }
    @Override public boolean isEnabled() { return activo; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + rol)); }
    @Override public String toString() { return "TecnicoPrincipal[" + username + "]"; }
}
