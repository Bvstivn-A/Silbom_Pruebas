package cl.silbom.backend;

import cl.silbom.backend.dto.BloqueoCreateDTO;
import cl.silbom.backend.dto.BloqueoResponseDTO;
import cl.silbom.backend.dto.MaquinaCreateDTO;
import cl.silbom.backend.dto.MaquinaResponseDTO;
import cl.silbom.backend.model.EstadoBloqueo;
import cl.silbom.backend.model.TipoIntervencion;
import cl.silbom.backend.service.BloqueoService;
import cl.silbom.backend.service.MaquinaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QUÉ HACE:
 * Suite de pruebas automatizadas de integración y reglas de negocio para SILBOM.
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - Valida de punta a punta las reglas críticas de LOTO, Multi-LOTO, MTBF, DS 594 y Ciberseguridad.
 * - Utiliza H2 en memoria con @ActiveProfiles("test") para que la compilación y pruebas
 *   se ejecuten sin necesidad de tener PostgreSQL iniciado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SilbomApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaquinaService maquinaService;

    @Autowired
    private BloqueoService bloqueoService;

    private static final String API_KEY = "test-api-key-12345";

    @Test
    @DisplayName("Control 1 y Seed: Carga inicial de las 6 máquinas de Planta San Joaquín")
    void testCargaInicialMaquinas() throws Exception {
        mockMvc.perform(get("/api/v1/maquinas")
                .header("User-Agent", "TestRunner/1.0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))))
            .andExpect(jsonPath("$[?(@.id == 'EQ-ENV-01')].nombre", contains("Envasadora Rotativa Automática")))
            .andExpect(jsonPath("$[?(@.id == 'EQ-ENV-01')].mtbf", contains(233.8)))
            .andExpect(jsonPath("$[?(@.id == 'EQ-PR-04')].mtbf", contains(356.0)));
    }

    @Test
    @DisplayName("Control 6: Exige API Key en POST /api/v1/maquinas (401 si falta)")
    void testProteccionApiKeyEnMutacion() throws Exception {
        MaquinaCreateDTO nueva = new MaquinaCreateDTO(
            "EQ-TEST-99", "Torno CNC Test", "Mecanizado", "⚙️", 100.0, 1, null
        );

        // Sin header X-API-KEY -> 401 Unauthorized
        mockMvc.perform(post("/api/v1/maquinas")
                .header("User-Agent", "TestRunner/1.0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nueva)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)));

        // Con header X-API-KEY válido -> 201 Created
        mockMvc.perform(post("/api/v1/maquinas")
                .header("User-Agent", "TestRunner/1.0")
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nueva)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is("EQ-TEST-99")))
            .andExpect(jsonPath("$.mtbf", is(100.0)));
    }

    @Test
    @DisplayName("Regla DS 594: Rechaza con 400 Bad Request si confirmacionCandadoDs594 es false")
    void testRechazoSinCandadoFisicoDs594() throws Exception {
        BloqueoCreateDTO dtoInvalido = new BloqueoCreateDTO(
            "EQ-ENV-01",
            TipoIntervencion.CORRECTIVA,
            "Falla de prueba sin candado",
            "Operario Prueba",
            false, // Violación de DS 594
            null,
            null
        );

        mockMvc.perform(post("/api/v1/bloqueos")
                .header("User-Agent", "TestRunner/1.0")
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoInvalido)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status", is(400)))
            .andExpect(jsonPath("$.mensaje", containsString("DS 594")));
    }

    @Test
    @DisplayName("Regla Multi-LOTO: Varios técnicos bloquean y la máquina se libera SOLO cuando todos salen")
    void testReglaMultiLotoConcurrente() {
        String maquinaId = "EQ-PR-04";

        // Estado inicial
        MaquinaResponseDTO inicial = maquinaService.obtenerPorId(maquinaId);
        assertFalse(inicial.bloqueada());
        assertEquals(0, inicial.bloqueosActivos());

        // Técnico 1 entra
        BloqueoResponseDTO b1 = bloqueoService.crearBloqueo(new BloqueoCreateDTO(
            maquinaId, TipoIntervencion.PREVENTIVA, "Mantención eléctrica", "Técnico 1", true, null, null
        ));
        MaquinaResponseDTO trasT1 = maquinaService.obtenerPorId(maquinaId);
        assertTrue(trasT1.bloqueada());
        assertEquals(1, trasT1.bloqueosActivos());

        // Técnico 2 entra (Multi-LOTO)
        BloqueoResponseDTO b2 = bloqueoService.crearBloqueo(new BloqueoCreateDTO(
            maquinaId, TipoIntervencion.PREVENTIVA, "Inspección mecánica", "Técnico 2", true, null, null
        ));
        MaquinaResponseDTO trasT2 = maquinaService.obtenerPorId(maquinaId);
        assertTrue(trasT2.bloqueada());
        assertEquals(2, trasT2.bloqueosActivos());

        // Técnico 1 se retira y libera su ticket
        bloqueoService.liberarBloqueo(b1.id());
        MaquinaResponseDTO trasSalidaT1 = maquinaService.obtenerPorId(maquinaId);
        // CRÍTICO: La máquina DEBE continuar bloqueada porque el Técnico 2 sigue dentro
        assertTrue(trasSalidaT1.bloqueada(), "La máquina debe permanecer bloqueada si bloqueosActivos > 0");
        assertEquals(1, trasSalidaT1.bloqueosActivos());

        // Técnico 2 se retira y libera su ticket
        bloqueoService.liberarBloqueo(b2.id());
        MaquinaResponseDTO trasSalidaT2 = maquinaService.obtenerPorId(maquinaId);
        // Ahora sí: bloqueosActivos == 0 -> bloqueada = false
        assertFalse(trasSalidaT2.bloqueada(), "La máquina debe desbloquearse cuando el último técnico se retira");
        assertEquals(0, trasSalidaT2.bloqueosActivos());
    }

    @Test
    @DisplayName("Regla MTBF: CORRECTIVA penaliza y suma falla, PREVENTIVA no penaliza")
    void testReglaMtbfCorrectivaVsPreventiva() {
        String maquinaId = "EQ-MEZ-01"; // Inicial: horas=715.0, fallas=1, mtbf=715.0

        MaquinaResponseDTO inicial = maquinaService.obtenerPorId(maquinaId);
        assertEquals(715.0, inicial.mtbf());
        assertEquals(1, inicial.fallasCorrectivas());

        // 1. Intervención PREVENTIVA -> no debe cambiar fallas ni MTBF
        BloqueoResponseDTO prev = bloqueoService.crearBloqueo(new BloqueoCreateDTO(
            maquinaId, TipoIntervencion.PREVENTIVA, "Lubricación rutinaria", "Técnico Mantención", true, null, null
        ));
        bloqueoService.liberarBloqueo(prev.id());

        MaquinaResponseDTO trasPrev = maquinaService.obtenerPorId(maquinaId);
        assertEquals(1, trasPrev.fallasCorrectivas(), "PREVENTIVA no debe aumentar fallas");
        assertEquals(715.0, trasPrev.mtbf(), "PREVENTIVA no debe alterar MTBF");

        // 2. Intervención CORRECTIVA -> debe sumar +1 a fallas (1 -> 2) y recalcular mtbf (715.0 / 2 = 357.5)
        BloqueoResponseDTO corr = bloqueoService.crearBloqueo(new BloqueoCreateDTO(
            maquinaId, TipoIntervencion.CORRECTIVA, "Rotura de eje motriz", "Técnico Mecánico", true, null, null
        ));
        bloqueoService.liberarBloqueo(corr.id());

        MaquinaResponseDTO trasCorr = maquinaService.obtenerPorId(maquinaId);
        assertEquals(2, trasCorr.fallasCorrectivas(), "CORRECTIVA debe sumar +1 a fallas");
        assertEquals(357.5, trasCorr.mtbf(), "MTBF debe ser 715.0 / 2 = 357.5");
    }

    @Test
    @DisplayName("Seguridad Industrial: Prohibido eliminar un bloqueo en estado ACTIVO (409 Conflict)")
    void testProhibidoEliminarBloqueoActivo() throws Exception {
        BloqueoResponseDTO activo = bloqueoService.crearBloqueo(new BloqueoCreateDTO(
            "EQ-EXT-02", TipoIntervencion.PREVENTIVA, "Revisión de boquilla", "Técnico Activo", true, null, null
        ));

        // Intento de borrado con DELETE -> 409 Conflict
        mockMvc.perform(delete("/api/v1/bloqueos/" + activo.id())
                .header("User-Agent", "TestRunner/1.0")
                .header("X-API-KEY", API_KEY))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status", is(409)))
            .andExpect(jsonPath("$.mensaje", containsString("ACTIVO")));

        // Si se libera primero, entonces el DELETE debe funcionar devolviendo 204 No Content
        bloqueoService.liberarBloqueo(activo.id());

        mockMvc.perform(delete("/api/v1/bloqueos/" + activo.id())
                .header("User-Agent", "TestRunner/1.0")
                .header("X-API-KEY", API_KEY))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Control 18: Cabeceras de seguridad HTTP presentes en la respuesta")
    void testCabecerasDeSeguridadPresentes() throws Exception {
        mockMvc.perform(get("/api/v1/maquinas")
                .header("User-Agent", "TestRunner/1.0"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("Content-Security-Policy", "default-src 'self'"))
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
    }
}
