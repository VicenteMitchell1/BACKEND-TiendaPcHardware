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
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema (Proxy hacia usuario-service)")
@RequiredArgsConstructor
public class UsuarioProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.usuario-url:http://localhost:8081}")
    private String usuarioUrl;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findAll() {
        return restTemplate.getForEntity(usuarioUrl + "/usuarios", String.class);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return restTemplate.getForEntity(usuarioUrl + "/usuarios/" + id, String.class);
    }

    @PostMapping
    @Operation(summary = "Crear nuevo usuario (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> create(@RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(usuarioUrl + "/usuarios", new HttpEntity<>(body, headers), String.class);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(usuarioUrl + "/usuarios/" + id, HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario (Exclusivo Administrador)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        restTemplate.delete(usuarioUrl + "/usuarios/" + id);
        return ResponseEntity.noContent().build();
    }
}
