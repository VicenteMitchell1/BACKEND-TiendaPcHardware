package cl.pchardware.usuario.controller;

import cl.pchardware.usuario.dto.UsuarioRequest;
import cl.pchardware.usuario.dto.UsuarioResponse;
import cl.pchardware.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuarios", description = "CRUD de usuarios (ADMIN)")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    public List<UsuarioResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public UsuarioResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @Operation(summary = "Crear usuario")
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario")
    public UsuarioResponse update(@PathVariable Long id, @Valid @RequestBody UsuarioRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
