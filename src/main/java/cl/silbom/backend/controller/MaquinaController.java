package cl.silbom.backend.controller;

import cl.silbom.backend.dto.MaquinaCreateDTO;
import cl.silbom.backend.dto.MaquinaResponseDTO;
import cl.silbom.backend.dto.MaquinaUpdateDTO;
import cl.silbom.backend.service.MaquinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * QUÉ HACE:
 * Controlador REST para el catálogo de máquinas industriales en Planta San Joaquín (/api/v1/maquinas).
 *
 * POR QUÉ ESTÁ ESCRITO ASÍ:
 * - Capa de Ruteo Pura: Se encarga únicamente de recibir peticiones HTTP, activar validaciones
 *   con @Valid y devolver códigos de estado HTTP precisos (200, 201, 204).
 * - CERO lógica de negocio: Toda decisión y regla industrial está delegada al MaquinaService.
 */
@RestController
@RequestMapping("/api/v1/maquinas")
public class MaquinaController {

    private final MaquinaService maquinaService;

    public MaquinaController(MaquinaService maquinaService) {
        this.maquinaService = maquinaService;
    }

    /**
     * GET /api/v1/maquinas: Lista todas las máquinas, opcionalmente filtrando por ?bloqueada=true/false.
     */
    @GetMapping
    public ResponseEntity<List<MaquinaResponseDTO>> listar(@RequestParam(required = false) Boolean bloqueada) {
        List<MaquinaResponseDTO> maquinas = maquinaService.listarMaquinas(bloqueada);
        return ResponseEntity.ok(maquinas);
    }

    /**
     * GET /api/v1/maquinas/{id}: Obtiene los detalles de una máquina específica.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MaquinaResponseDTO> obtenerPorId(@PathVariable String id) {
        MaquinaResponseDTO maquina = maquinaService.obtenerPorId(id);
        return ResponseEntity.ok(maquina);
    }

    /**
     * POST /api/v1/maquinas: Da de alta un nuevo equipo industrial en el catálogo.
     */
    @PostMapping
    public ResponseEntity<MaquinaResponseDTO> crear(@Valid @RequestBody MaquinaCreateDTO dto) {
        MaquinaResponseDTO creada = maquinaService.crearMaquina(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * PUT /api/v1/maquinas/{id}: Actualiza datos operativos y recalcula MTBF.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MaquinaResponseDTO> actualizar(@PathVariable String id,
                                                         @Valid @RequestBody MaquinaUpdateDTO dto) {
        MaquinaResponseDTO actualizada = maquinaService.actualizarMaquina(id, dto);
        return ResponseEntity.ok(actualizada);
    }

    /**
     * DELETE /api/v1/maquinas/{id}: Elimina una máquina si no está en estado de bloqueo.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        maquinaService.eliminarMaquina(id);
        return ResponseEntity.noContent().build();
    }
}
