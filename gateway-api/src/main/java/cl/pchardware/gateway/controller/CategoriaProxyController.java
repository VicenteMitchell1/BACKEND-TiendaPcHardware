package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.dto.doc.CategoriaDocs;
import cl.pchardware.gateway.dto.doc.ErrorDoc;
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
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorias", description = "Categorías de hardware (lectura pública, escritura ADMIN) → categoria-service")
@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "404", description = "No encontrada", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "503", description = "Servicio no disponible", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@RequiredArgsConstructor
public class CategoriaProxyController {

    private final ProxyService proxy;

    @Value("${services.categoria-url:http://localhost:8082}")
    private String categoriaUrl;

    @GetMapping
    @Operation(summary = "Listar todas las categorías (público)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoriaDocs.Response.class))))
    public ResponseEntity<String> findAll() {
        return proxy.get(categoriaUrl + "/categorias");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID (público)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CategoriaDocs.Response.class)))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return proxy.get(categoriaUrl + "/categorias/" + id);
    }

    @PostMapping
    @Operation(summary = "Crear categoría (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CategoriaDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = CategoriaDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.POST, categoriaUrl + "/categorias", body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría (ADMIN). Un nuevo nombre se propaga a sus productos", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CategoriaDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "Nombre duplicado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> update(@PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = CategoriaDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.PUT, categoriaUrl + "/categorias/" + id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría (ADMIN). No se permite si tiene productos", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "409", description = "La categoría tiene productos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return proxy.forward(HttpMethod.DELETE, categoriaUrl + "/categorias/" + id, null);
    }
}
