package cl.pchardware.boleta.controller;

import cl.pchardware.boleta.dto.CuponRequest;
import cl.pchardware.boleta.dto.CuponResponse;
import cl.pchardware.boleta.dto.UsuarioActual;
import cl.pchardware.boleta.service.CuponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cupones")
@Tag(name = "Cupones", description = "Cupones de descuento: CRUD para ADMIN y validación para el carrito")
@RequiredArgsConstructor
public class CuponController {

    private final CuponService service;

    @GetMapping
    @Operation(summary = "Listar cupones (ADMIN)")
    public List<CuponResponse> findAll(@Parameter(hidden = true) UsuarioActual usuario) {
        return service.findAll(usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cupón por ID (ADMIN)")
    public CuponResponse findById(@PathVariable Long id, @Parameter(hidden = true) UsuarioActual usuario) {
        return service.findById(id, usuario);
    }

    @GetMapping("/validar/{codigo}")
    @Operation(summary = "Validar un código de cupón activo (usuarios autenticados)")
    public CuponResponse validar(@PathVariable String codigo) {
        return service.validar(codigo);
    }

    @PostMapping
    @Operation(summary = "Crear cupón (ADMIN)")
    public ResponseEntity<CuponResponse> create(@Valid @RequestBody CuponRequest request,
                                                @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, usuario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cupón (ADMIN)")
    public CuponResponse update(@PathVariable Long id, @Valid @RequestBody CuponRequest request,
                                @Parameter(hidden = true) UsuarioActual usuario) {
        return service.update(id, request, usuario);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cupón (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id, @Parameter(hidden = true) UsuarioActual usuario) {
        service.delete(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
