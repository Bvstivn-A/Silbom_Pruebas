package cl.silbom.backend.security;

import cl.silbom.backend.repository.TecnicoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TecnicoUserDetailsService implements UserDetailsService {
    private final TecnicoRepository repository;
    private final String planta;
    public TecnicoUserDetailsService(TecnicoRepository repository, @Value("${silbom.security.planta-id}") String planta) {
        this.repository = repository; this.planta = planta;
    }
    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return repository.findByUsernameAndPlantaId(username, planta).map(TecnicoPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Credencial inválida"));
    }
}
