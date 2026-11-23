package cl.pchardware.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Gestión de catálogo de productos (Proxy hacia producto-service)")
@RequiredArgsConstructor
public class ProductoProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.producto-url:http://localhost:8083}")
    private String productoUrl;

    @GetMapping
    @Operation(summary = "Listar todos los productos")
    public ResponseEntity<String> findAll() {
        return restTemplate.getForEntity(productoUrl + "/productos", String.class);
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar solo productos activos")
    public ResponseEntity<String> findActivos() {
        return restTemplate.getForEntity(productoUrl + "/productos/activos", String.class);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID")
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return restTemplate.getForEntity(productoUrl + "/productos/" + id, String.class);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> create(@RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(productoUrl + "/productos", new HttpEntity<>(body, headers), String.class);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto existente (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(productoUrl + "/productos/" + id, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        restTemplate.delete(productoUrl + "/productos/" + id);
        return ResponseEntity.noContent().build();
    }
}
