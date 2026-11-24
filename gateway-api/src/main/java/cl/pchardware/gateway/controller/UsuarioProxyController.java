package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.dto.doc.ErrorDoc;
import cl.pchardware.gateway.dto.doc.UsuarioDocs;
import cl.pchardware.gateway.proxy.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles (exclusivo ADMIN) → usuario-service")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "401", description = "Sin token o token expirado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@RequiredArgsConstructor
public class UsuarioProxyController {

    private final ProxyService proxy;

    @Value("${services.usuario-url:http://localhost:8081}")
    private String usuarioUrl;

    @GetMapping
    @Operation(summary = "Listar usuarios (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = UsuarioDocs.Response.class))))
    public ResponseEntity<String> findAll() {
        return proxy.get(usuarioUrl + "/usuarios");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UsuarioDocs.Response.class)))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return proxy.get(usuarioUrl + "/usuarios/" + id);
    }

    @PostMapping
    @Operation(summary = "Crear usuario con cualquier rol (ADMIN)")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = UsuarioDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "Email ya registrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = UsuarioDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.POST, usuarioUrl + "/usuarios", body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario, rol o estado activo (ADMIN)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UsuarioDocs.Response.class)))
    public ResponseEntity<String> update(@PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = UsuarioDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.PUT, usuarioUrl + "/usuarios/" + id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return proxy.forward(HttpMethod.DELETE, usuarioUrl + "/usuarios/" + id, null);
    }
}
