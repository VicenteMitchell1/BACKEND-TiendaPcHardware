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
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias", description = "Gestión de categorías (Proxy hacia categoria-service)")
@RequiredArgsConstructor
public class CategoriaProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.categoria-url:http://localhost:8082}")
    private String categoriaUrl;

    @GetMapping
    @Operation(summary = "Listar todas las categorías")
    public ResponseEntity<String> findAll() {
        return restTemplate.getForEntity(categoriaUrl + "/categorias", String.class);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID")
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return restTemplate.getForEntity(categoriaUrl + "/categorias/" + id, String.class);
    }

    @PostMapping
    @Operation(summary = "Crear nueva categoría (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> create(@RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(categoriaUrl + "/categorias", new HttpEntity<>(body, headers), String.class);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(categoriaUrl + "/categorias/" + id, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        restTemplate.delete(categoriaUrl + "/categorias/" + id);
        return ResponseEntity.noContent().build();
    }
}
