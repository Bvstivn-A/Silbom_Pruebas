package cl.silbom.backend.controller;
import cl.silbom.backend.dto.*;
import cl.silbom.backend.service.TecnicoService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/tecnicos")
public class TecnicoController {
    private final TecnicoService service;
    public TecnicoController(TecnicoService service) { this.service=service; }
    @GetMapping public List<TecnicoResponseDTO> listar() { return service.listar(); }
    @GetMapping("/{id}") public TecnicoResponseDTO obtener(@PathVariable Long id) { return service.obtener(id); }
    @PostMapping public ResponseEntity<TecnicoResponseDTO> crear(@Valid @RequestBody TecnicoCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }
    @PutMapping("/{id}") public TecnicoResponseDTO actualizar(@PathVariable Long id,@Valid @RequestBody TecnicoUpdateDTO dto) { return service.actualizar(id,dto); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { service.eliminar(id); return ResponseEntity.noContent().build(); }
}
