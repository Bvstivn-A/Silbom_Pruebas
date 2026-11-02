package cl.silbom.backend.config;

import cl.silbom.backend.model.BloqueoLoto;
import cl.silbom.backend.model.EstadoBloqueo;
import cl.silbom.backend.model.Maquina;
import cl.silbom.backend.model.TipoIntervencion;
import cl.silbom.backend.repository.BloqueoLotoRepository;
import cl.silbom.backend.repository.MaquinaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * QUÉ HACE:
 * Inicializador automático de datos que se ejecuta al arrancar el servidor Spring Boot.
 * Carga el inventario de las 6 máquinas base de Planta San Joaquín y los 3 tickets históricos
 * cerrados si la base de datos se encuentra vacía.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * Garantiza coincidencia 100% con los datos prototipados en el Hito 1 (Silbom1.html)
 * facilitando la demostración en vivo ante la comisión sin requerir pasos manuales previos.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MaquinaRepository maquinaRepository;
    private final BloqueoLotoRepository bloqueoLotoRepository;
    private final String plantaId;

    public DataInitializer(MaquinaRepository maquinaRepository,
                           BloqueoLotoRepository bloqueoLotoRepository,
                           @Value("${silbom.security.planta-id:PLANTA-SAN-JOAQUIN}") String plantaId) {
        this.maquinaRepository = maquinaRepository;
        this.bloqueoLotoRepository = bloqueoLotoRepository;
        this.plantaId = plantaId;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (maquinaRepository.count() == 0) {
            log.info("Inicializando datos maestros de Planta San Joaquín...");

            List<Maquina> maquinasIniciales = List.of(
                new Maquina("EQ-ENV-01", "Envasadora Rotativa Automática", "Línea de Envasado 1", "📦", 701.5, 3, this.plantaId),
                new Maquina("EQ-PR-04",  "Prensa Hidráulica 200T",         "Área de Estampado",   "⚙️", 712.0, 2, this.plantaId),
                new Maquina("EQ-EXT-02", "Extrusora de Polímeros",          "Área de Moldeo",      "🔄", 685.2, 4, this.plantaId),
                new Maquina("EQ-MEZ-01", "Mezclador Industrial de Cinta",   "Área de Preparación", "🥣", 715.0, 1, this.plantaId),
                new Maquina("EQ-COM-03", "Compresor de Tornillo",           "Sala de Máquinas",    "💨", 718.5, 1, this.plantaId),
                new Maquina("EQ-TR-01",  "Cinta Transportadora Principal",  "Logística y Despacho","🏗️", 708.0, 2, this.plantaId)
            );

            maquinaRepository.saveAll(maquinasIniciales);
            log.info("Se cargaron exitosamente las 6 máquinas industriales.");

            // Cargar los 3 bloqueos cerrados iniciales
            LocalDateTime ahora = LocalDateTime.now();
            List<BloqueoLoto> historialInicial = List.of(
                new BloqueoLoto(
                    "EQ-PR-04",
                    TipoIntervencion.PREVENTIVA,
                    "Inspección y lubricación semanal programada",
                    "Carlos R.",
                    true,
                    EstadoBloqueo.CERRADO,
                    ahora.minusDays(3),
                    ahora.minusDays(3).plusMinutes(45),
                    45L,
                    this.plantaId
                ),
                new BloqueoLoto(
                    "EQ-EXT-02",
                    TipoIntervencion.CORRECTIVA,
                    "Sobrecalentamiento en motor principal (Eléctrico)",
                    "Vicente M.",
                    true,
                    EstadoBloqueo.CERRADO,
                    ahora.minusDays(2),
                    ahora.minusDays(2).plusMinutes(72),
                    72L,
                    this.plantaId
                ),
                new BloqueoLoto(
                    "EQ-ENV-01",
                    TipoIntervencion.CORRECTIVA,
                    "Atascamiento de cabezal móvil (Mecánico)",
                    "Benjamín V.",
                    true,
                    EstadoBloqueo.CERRADO,
                    ahora.minusDays(1),
                    ahora.minusDays(1).plusMinutes(35),
                    35L,
                    this.plantaId
                )
            );

            bloqueoLotoRepository.saveAll(historialInicial);
            log.info("Se cargaron exitosamente los 3 tickets históricos cerrados.");
        } else {
            log.info("La base de datos ya contiene registros maestros (total: {} máquinas). Omitiendo seed inicial.", maquinaRepository.count());
        }
    }
}
