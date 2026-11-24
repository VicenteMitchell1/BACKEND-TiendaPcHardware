package cl.pchardware.producto.controller;

import cl.pchardware.producto.dto.CategoriaNombreRequest;
import cl.pchardware.producto.dto.MovimientoStockRequest;
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
import java.util.Map;

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
    @Operation(summary = "Crear producto (valida que la categoría exista)")
    public ResponseEntity<ProductoResponse> create(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public ProductoResponse update(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Endpoints internos (boleta-service y categoria-service) ----------

    @RequestMapping(value = "/{id}/stock", method = {RequestMethod.PATCH, RequestMethod.POST})
    @Operation(summary = "Ajustar stock de un producto (positivo repone, negativo descuenta)")
    public ResponseEntity<Void> updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest req) {
        service.updateStock(id, req.getCantidad());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/stock/descontar")
    @Operation(summary = "Descontar stock de una compra completa (todo o nada); devuelve los precios vigentes")
    public List<ProductoResponse> descontarStock(@Valid @RequestBody MovimientoStockRequest req) {
        return service.descontarStock(req);
    }

    @PostMapping("/stock/reponer")
    @Operation(summary = "Reponer stock de una compra anulada o fallida")
    public ResponseEntity<Void> reponerStock(@Valid @RequestBody MovimientoStockRequest req) {
        service.reponerStock(req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categoria/{idCategoria}/total")
    @Operation(summary = "Cantidad de productos de una categoría")
    public Map<String, Long> contarPorCategoria(@PathVariable Long idCategoria) {
        return Map.of("total", service.contarPorCategoria(idCategoria));
    }

    @PutMapping("/categoria/{idCategoria}/nombre")
    @Operation(summary = "Sincronizar el nombre de la categoría en sus productos")
    public Map<String, Integer> sincronizarNombreCategoria(@PathVariable Long idCategoria,
                                                           @Valid @RequestBody CategoriaNombreRequest req) {
        return Map.of("actualizados", service.sincronizarNombreCategoria(idCategoria, req.getNombre()));
    }
}
