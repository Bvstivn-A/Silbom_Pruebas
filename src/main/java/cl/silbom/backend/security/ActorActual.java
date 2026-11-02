package cl.silbom.backend.security;

import cl.silbom.backend.exception.*;
import cl.silbom.backend.model.Rol;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorActual {
    public TecnicoPrincipal obtener() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof TecnicoPrincipal t))
            throw new SecurityAuthException("Se requiere la identidad individual del técnico");
        return t;
    }
    public void exigirSupervisor() {
        if (obtener().rol() != Rol.SUPERVISOR) throw new ForbiddenOperationException("Operación reservada a supervisores");
    }
    public void exigirPropietario(Long tecnicoId) {
        if (!obtener().id().equals(tecnicoId))
            throw new ForbiddenOperationException("Solo el técnico responsable puede liberar este bloqueo; los registros legados requieren un procedimiento supervisado");
    }
}
