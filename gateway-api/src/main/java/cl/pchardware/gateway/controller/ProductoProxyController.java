package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.dto.doc.ErrorDoc;
import cl.pchardware.gateway.dto.doc.ProductoDocs;
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
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo de productos (lectura pública, escritura ADMIN) → producto-service")
@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "503", description = "Servicio no disponible", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@RequiredArgsConstructor
public class ProductoProxyController {

    private final ProxyService proxy;

    @Value("${services.producto-url:http://localhost:8083}")
    private String productoUrl;

    @GetMapping
    @Operation(summary = "Listar todos los productos (público)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductoDocs.Response.class))))
    public ResponseEntity<String> findAll() {
        return proxy.get(productoUrl + "/productos");
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar sólo productos activos (público)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ProductoDocs.Response.class))))
    public ResponseEntity<String> findActivos() {
        return proxy.get(productoUrl + "/productos/activos");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID (público)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductoDocs.Response.class)))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return proxy.get(productoUrl + "/productos/" + id);
    }

    @PostMapping
    @Operation(summary = "Crear producto (ADMIN). La categoría debe existir", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = ProductoDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "SKU duplicado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ProductoDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.POST, productoUrl + "/productos", body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductoDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "SKU duplicado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> update(@PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = ProductoDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.PUT, productoUrl + "/productos/" + id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto (ADMIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Eliminado")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return proxy.forward(HttpMethod.DELETE, productoUrl + "/productos/" + id, null);
    }
}
