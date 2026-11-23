package cl.pchardware.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/detalles")
@Tag(name = "Detalles Boleta", description = "Líneas de detalle de boletas (Proxy hacia detalle-service)")
@RequiredArgsConstructor
public class DetalleProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.detalle-url:http://localhost:8085}")
    private String detalleUrl;

    @GetMapping
    @Operation(summary = "Listar todos los detalles (ADMIN y VENDEDOR)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findAll() {
        return restTemplate.getForEntity(detalleUrl + "/detalles", String.class);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle por ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return restTemplate.getForEntity(detalleUrl + "/detalles/" + id, String.class);
    }

    @GetMapping("/boleta/{idBoleta}")
    @Operation(summary = "Obtener detalles de una boleta", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findByBoletaId(@PathVariable Long idBoleta) {
        return restTemplate.getForEntity(detalleUrl + "/detalles/boleta/" + idBoleta, String.class);
    }
}
