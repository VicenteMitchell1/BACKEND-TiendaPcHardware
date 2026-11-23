package cl.pchardware.categoria.controller;

import cl.pchardware.categoria.dto.CategoriaRequest;
import cl.pchardware.categoria.dto.CategoriaResponse;
import cl.pchardware.categoria.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@Tag(name = "Categorias", description = "CRUD de categorias de productos")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    @Operation(summary = "Listar todas las categorias")
    public List<CategoriaResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoria por ID")
    public CategoriaResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @Operation(summary = "Crear categoria")
    public ResponseEntity<CategoriaResponse> create(@Valid @RequestBody CategoriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoria")
    public CategoriaResponse update(@PathVariable Long id, @Valid @RequestBody CategoriaRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoria")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
