package cl.pchardware.producto.controller;

import cl.pchardware.producto.dto.ProductoRequest;
import cl.pchardware.producto.dto.ProductoResponse;
import cl.pchardware.producto.dto.StockUpdateRequest;
import cl.pchardware.producto.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "CRUD de productos con gestión de stock")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    @GetMapping
    @Operation(summary = "Listar todos los productos")
    public List<ProductoResponse> findAll() { return service.findAll(); }

    @GetMapping("/activos")
    @Operation(summary = "Listar solo productos activos")
    public List<ProductoResponse> findActivos() { return service.findActivos(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    public ProductoResponse findById(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    @Operation(summary = "Crear producto")
    public ResponseEntity<ProductoResponse> create(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public ProductoResponse update(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.update(id, req);
    }

    @RequestMapping(value = "/{id}/stock", method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Actualizar stock (llamado interno por boleta-service)")
    public ResponseEntity<Void> updateStock(@PathVariable Long id, @RequestBody StockUpdateRequest req) {
        service.updateStock(id, req.getCantidad());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
