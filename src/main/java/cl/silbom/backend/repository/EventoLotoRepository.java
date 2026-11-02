package cl.silbom.backend.repository;
import cl.silbom.backend.model.EventoLoto;
import org.springframework.data.repository.Repository;
import java.util.List;
public interface EventoLotoRepository extends Repository<EventoLoto, Long> {
    EventoLoto save(EventoLoto evento);
    List<EventoLoto> findByBloqueoIdAndPlantaIdOrderById(Long bloqueoId, String plantaId);
}
