package cl.silbom.backend.repository;
import cl.silbom.backend.model.Falla;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FallaRepository extends JpaRepository<Falla, Long> {
    Optional<Falla> findByMaquinaIdAndPlantaIdAndReferencia(String maquinaId, String plantaId, String referencia);
}
