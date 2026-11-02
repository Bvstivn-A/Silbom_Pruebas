package cl.silbom.backend.service;

import cl.silbom.backend.dto.*;
import cl.silbom.backend.exception.*;
import cl.silbom.backend.model.*;
import cl.silbom.backend.repository.*;
import cl.silbom.backend.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TecnicoService {
    private final TecnicoRepository repository;
    private final BloqueoLotoRepository bloqueos;
    private final PasswordEncoder encoder;
    private final ActorActual actor;
    private final String planta;
    public TecnicoService(TecnicoRepository repository, BloqueoLotoRepository bloqueos, PasswordEncoder encoder,
                         ActorActual actor, @Value("${silbom.security.planta-id}") String planta) {
        this.repository=repository; this.bloqueos=bloqueos; this.encoder=encoder; this.actor=actor; this.planta=planta;
    }
    @Transactional(readOnly=true)
    public List<TecnicoResponseDTO> listar() {
        actor.exigirSupervisor();
        return repository.findByPlantaIdAndActivoTrueOrderById(planta).stream().map(TecnicoResponseDTO::from).toList();
    }
    @Transactional(readOnly=true)
    public TecnicoResponseDTO obtener(Long id) {
        actor.exigirSupervisor();
        return repository.findByIdAndPlantaIdAndActivoTrue(id,planta).map(TecnicoResponseDTO::from).orElseThrow(()->new ResourceNotFoundException("Técnico no encontrado"));
    }
    @Transactional
    public TecnicoResponseDTO crear(TecnicoCreateDTO dto) {
        actor.exigirSupervisor();
        if (repository.findByUsernameAndPlantaId(dto.username(),planta).isPresent())
            throw new ResourceAlreadyExistsException("Nombre de usuario ya utilizado, incluso si está inactivo");
        var t=new Tecnico(dto.username(), InputSanitizer.required(dto.nombre(),3,80), hash(dto.password()),dto.rol(),planta);
        return TecnicoResponseDTO.from(repository.save(t));
    }
    @Transactional
    public TecnicoResponseDTO actualizar(Long id, TecnicoUpdateDTO dto) {
        actor.exigirSupervisor();
        var t=repository.findForUpdate(id,planta).orElseThrow(()->new ResourceNotFoundException("Técnico no encontrado"));
        if (id.equals(actor.obtener().id()) && dto.rol()!=Rol.SUPERVISOR)
            throw new BusinessConflictException("No puede retirar su propio rol supervisor");
        t.actualizar(InputSanitizer.required(dto.nombre(),3,80),dto.rol(),dto.password()==null?null:hash(dto.password()));
        return TecnicoResponseDTO.from(t);
    }
    @Transactional
    public void eliminar(Long id) {
        actor.exigirSupervisor();
        var t=repository.findForUpdate(id,planta).orElseThrow(()->new ResourceNotFoundException("Técnico no encontrado"));
        if (id.equals(actor.obtener().id())) throw new BusinessConflictException("No puede dar de baja su propia cuenta supervisora");
        if (bloqueos.existsByTecnicoIdAndEstado(id,EstadoBloqueo.ACTIVO))
            throw new BusinessConflictException("El técnico tiene bloqueos activos");
        t.desactivar();
    }
    private String hash(String password) {
        if (password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("La contraseña requiere al menos 12 caracteres y como máximo 72 bytes UTF-8");
        return encoder.encode(password);
    }
}
