package cl.pchardware.gateway.controller;

import cl.pchardware.gateway.dto.doc.BoletaDocs;
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

/**
 * Lectura para ADMIN y VENDEDOR. Escritura sólo ADMIN, pensada para correcciones administrativas
 * (no mueve stock ni recalcula el total de la boleta; para revertir una venta se anula la boleta).
 */
@RestController
@RequestMapping("/api/v1/detalles")
@Tag(name = "Detalles Boleta", description = "Líneas de detalle de las boletas → detalle-service")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token o token expirado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "403", description = "Rol sin permiso", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@RequiredArgsConstructor
public class DetalleProxyController {

    private final ProxyService proxy;

    @Value("${services.detalle-url:http://localhost:8085}")
    private String detalleUrl;

    @GetMapping
    @Operation(summary = "Listar todos los detalles (ADMIN, VENDEDOR)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoletaDocs.Detalle.class))))
    public ResponseEntity<String> findAll() {
        return proxy.get(detalleUrl + "/detalles");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle por ID (ADMIN, VENDEDOR)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BoletaDocs.Detalle.class)))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return proxy.get(detalleUrl + "/detalles/" + id);
    }

    @GetMapping("/boleta/{idBoleta}")
    @Operation(summary = "Obtener los detalles de una boleta (ADMIN, VENDEDOR)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoletaDocs.Detalle.class))))
    public ResponseEntity<String> findByBoletaId(@PathVariable Long idBoleta) {
        return proxy.get(detalleUrl + "/detalles/boleta/" + idBoleta);
    }

    @PostMapping
    @Operation(summary = "Agregar una línea de detalle (ADMIN, corrección administrativa)")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = BoletaDocs.Detalle.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = BoletaDocs.DetalleRequest.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.POST, detalleUrl + "/detalles", body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una línea de detalle (ADMIN); el subtotal se recalcula")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BoletaDocs.Detalle.class)))
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> update(@PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = BoletaDocs.DetalleRequest.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.PUT, detalleUrl + "/detalles/" + id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una línea de detalle (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return proxy.forward(HttpMethod.DELETE, detalleUrl + "/detalles/" + id, null);
    }
}
