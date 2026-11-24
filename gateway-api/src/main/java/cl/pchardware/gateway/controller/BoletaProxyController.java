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

/** La identidad del usuario (X-User-*) se agrega en ProxyService desde el JWT; boleta-service valida la propiedad. */
@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Compras y órdenes → boleta-service")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token o token expirado", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "403", description = "Rol sin permiso o boleta de otro cliente", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "404", description = "No encontrada", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@ApiResponse(responseCode = "503", description = "Servicio no disponible", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
@RequiredArgsConstructor
public class BoletaProxyController {

    private final ProxyService proxy;

    @Value("${services.boleta-url:http://localhost:8084}")
    private String boletaUrl;

    @GetMapping
    @Operation(summary = "Listar todas las boletas (ADMIN, VENDEDOR)")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoletaDocs.Response.class))))
    public ResponseEntity<String> findAll() {
        return proxy.get(boletaUrl + "/boletas");
    }

    @GetMapping("/mis-boletas")
    @Operation(summary = "Listar las boletas del usuario autenticado")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BoletaDocs.Response.class))))
    public ResponseEntity<String> findMisBoletas() {
        return proxy.get(boletaUrl + "/boletas/mis-boletas");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener boleta por ID (ADMIN y VENDEDOR: cualquiera; CLIENTE: sólo las suyas)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BoletaDocs.Response.class)))
    public ResponseEntity<String> findById(@PathVariable Long id) {
        return proxy.get(boletaUrl + "/boletas/" + id);
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Buscar boleta por número (CLIENTE: sólo las suyas)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BoletaDocs.Response.class)))
    public ResponseEntity<String> findByNumero(@PathVariable String numero) {
        return proxy.get(boletaUrl + "/boletas/numero/" + numero);
    }

    @PostMapping
    @Operation(summary = "Emitir boleta de compra: el total se calcula con los precios del catálogo y el stock se descuenta de forma atómica")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = BoletaDocs.Response.class)))
    @ApiResponse(responseCode = "400", description = "Stock insuficiente, producto inactivo o datos inválidos", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = BoletaDocs.Request.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.POST, boletaUrl + "/boletas", body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado o datos de despacho (ADMIN). ANULADA repone el stock")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = BoletaDocs.Response.class)))
    @ApiResponse(responseCode = "409", description = "La boleta ya está anulada", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> update(@PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = BoletaDocs.UpdateRequest.class)))
            @RequestBody String body) {
        return proxy.forward(HttpMethod.PUT, boletaUrl + "/boletas/" + id, body);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una boleta ANULADA junto a sus detalles (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "409", description = "La boleta no está anulada", content = @Content(schema = @Schema(implementation = ErrorDoc.class)))
    public ResponseEntity<String> delete(@PathVariable Long id) {
        return proxy.forward(HttpMethod.DELETE, boletaUrl + "/boletas/" + id, null);
    }
}
