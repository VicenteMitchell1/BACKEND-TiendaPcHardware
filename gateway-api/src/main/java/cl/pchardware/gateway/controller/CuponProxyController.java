package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.dto.doc.CuponDocs;
import cl.pchardware.gateway.dto.doc.ErrorDoc;
import cl.pchardware.gateway.proxy.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cupones")
@Tag(name = "Cupones", description = "Cupones de descuento (validación para usuarios autenticados, CRUD ADMIN) → boleta-service")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token o token inválido", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "503", description = "Servicio no disponible", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@RequiredArgsConstructor
public class CuponProxyController {

    private final ProxyService proxy;

    @Value("${services.boleta-url:http://localhost:8084}")
    private String boletaUrl;

    @GetMapping
    @Operation(summary = "Listar cupones (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CuponDocs.Response.class))))
    public ResponseEntity<String> findAll() {
        return proxy.get(boletaUrl + "/cupones");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cupón por ID (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CuponDocs.Response.class)))
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return proxy.get(boletaUrl + "/cupones/" + id);
    }

    /** El código se restringe al mismo alfabeto que admite el cupón: así se reenvía sin riesgo de inyectar rutas. */
    @GetMapping("/validar/{codigo}")
    @Operation(summary = "Validar un código de cupón activo (usuarios autenticados)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CuponDocs.Response.class)))
    @ApiResponse(responseCode = "400", description = "Formato de código inválido", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    @ApiResponse(responseCode = "404", description = "Código no válido o inactivo", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> validar(
            @PathVariable @Pattern(regexp = "^[A-Za-z0-9_-]{1,30}$", message = "Código de descuento con formato inválido") String codigo) {
        return proxy.get(boletaUrl + "/cupones/validar/" + codigo);
    }

    @PostMapping
    @Operation(summary = "Crear cupón (ADMIN)")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CuponDocs.Response.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    @ApiResponse(responseCode = "409", description = "Código duplicado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = CuponDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.POST, boletaUrl + "/cupones", body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cupón (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CuponDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "Código duplicado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> update(@PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = CuponDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.PUT, boletaUrl + "/cupones/" + id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cupón (ADMIN). Las boletas emitidas conservan su descuento")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return proxy.forward(HttpMethod.DELETE, boletaUrl + "/cupones/" + id, null);
    }
}
