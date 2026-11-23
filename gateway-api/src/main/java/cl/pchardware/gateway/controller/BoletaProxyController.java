package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Emisión y consulta de boletas (Proxy hacia boleta-service)")
@RequiredArgsConstructor
public class BoletaProxyController {

    private final RestTemplate restTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${services.boleta-url:http://localhost:8084}")
    private String boletaUrl;

    @GetMapping
    @Operation(summary = "Listar todas las boletas (ADMIN y VENDEDOR)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findAll() {
        return restTemplate.getForEntity(boletaUrl + "/boletas", String.class);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener boleta por ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return restTemplate.getForEntity(boletaUrl + "/boletas/" + id, String.class);
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Buscar boleta por número", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findByNumero(@PathVariable String numero) {
        return restTemplate.getForEntity(boletaUrl + "/boletas/numero/" + numero, String.class);
    }

    @GetMapping("/mis-boletas")
    @Operation(summary = "Listar boletas del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> findMisBoletas(HttpServletRequest request) {
        HttpHeaders headers = extractUserHeaders(request);
        return restTemplate.exchange(boletaUrl + "/boletas/mis-boletas", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @PostMapping
    @Operation(summary = "Emitir nueva boleta de compra", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<String> create(@RequestBody String body, HttpServletRequest request) {
        HttpHeaders headers = extractUserHeaders(request);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(boletaUrl + "/boletas", new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders extractUserHeaders(HttpServletRequest req) {
        HttpHeaders headers = new HttpHeaders();
        String authHeader = req.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validarToken(token)) {
                String email = jwtTokenProvider.getEmailFromToken(token);
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String rol = jwtTokenProvider.getRolFromToken(token);
                if (email != null) headers.set("X-User-Email", email);
                if (userId != null) headers.set("X-User-Id", String.valueOf(userId));
                if (rol != null) headers.set("X-User-Role", rol);
            }
        }
        return headers;
    }
}
