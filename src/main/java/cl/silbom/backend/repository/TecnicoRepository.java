package cl.silbom.backend.repository;

import cl.silbom.backend.model.Tecnico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {
    Optional<Tecnico> findByIdAndPlantaIdAndActivoTrue(Long id, String plantaId);
    Optional<Tecnico> findByUsernameAndPlantaId(String username, String plantaId);
    List<Tecnico> findByPlantaIdAndActivoTrueOrderById(String plantaId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tecnico t where t.id=:id and t.plantaId=:planta and t.activo=true")
    Optional<Tecnico> findForUpdate(@Param("id") Long id, @Param("planta") String planta);
}
