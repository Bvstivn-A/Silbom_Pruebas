package cl.silbom.backend.config;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** SET LOCAL en la misma conexión/transacción; el pool no conserva contexto de otra petición. */
@Aspect @Component @Order(1) @EnableTransactionManagement(order = 0)
public class PlantaTransactionAspect {
    private final EntityManager entityManager;
    private final String planta;
    public PlantaTransactionAspect(EntityManager entityManager, @Value("${silbom.security.planta-id}") String planta) {
        this.entityManager = entityManager; this.planta = planta;
    }
    @Around("execution(* cl.silbom.backend..*(..)) && @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object ejecutar(ProceedingJoinPoint call) throws Throwable {
        entityManager.unwrap(Session.class).doWork(connection -> {
            if ("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                try (var statement = connection.prepareStatement("SELECT set_config('app.current_planta_id', ?, true)")) {
                    statement.setString(1, planta);
                    statement.execute();
                }
            }
        });
        return call.proceed();
    }
}
