package cl.pchardware.boleta.controller;

import cl.pchardware.boleta.dto.BoletaRequest;
import cl.pchardware.boleta.dto.BoletaResponse;
import cl.pchardware.boleta.dto.BoletaUpdateRequest;
import cl.pchardware.boleta.dto.UsuarioActual;
import cl.pchardware.boleta.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * {@link UsuarioActual} se construye con las cabeceras X-User-* que agrega el API Gateway a partir del JWT
 * (ver UsuarioActualResolver); el frontend no las envía.
 */
@RestController
@RequestMapping("/boletas")
@Tag(name = "Boletas", description = "Emisión y consulta de boletas y órdenes de compra")
@RequiredArgsConstructor
public class BoletaController {

    private final BoletaService service;

    @GetMapping
    @Operation(summary = "Listar todas las boletas emitidas (ADMIN y VENDEDOR)")
    public List<BoletaResponse> findAll(@Parameter(hidden = true) UsuarioActual usuario) {
        return service.findAll(usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener boleta por ID (un CLIENTE sólo puede ver las suyas)")
    public BoletaResponse findById(@PathVariable Long id, @Parameter(hidden = true) UsuarioActual usuario) {
        return service.findById(id, usuario);
    }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Buscar boleta por número (un CLIENTE sólo puede ver las suyas)")
    public BoletaResponse findByNumero(@PathVariable String numero, @Parameter(hidden = true) UsuarioActual usuario) {
        return service.findByNumero(numero, usuario);
    }

    @GetMapping("/mis-boletas")
    @Operation(summary = "Listar boletas del usuario autenticado")
    public List<BoletaResponse> findMisBoletas(@Parameter(hidden = true) UsuarioActual usuario) {
        return service.findMisBoletas(usuario);
    }

    @PostMapping
    @Operation(summary = "Emitir nueva boleta: montos calculados en el servidor y descuento atómico de stock")
    public ResponseEntity<BoletaResponse> create(@Valid @RequestBody BoletaRequest request,
                                                 @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, usuario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estado y datos de despacho (ADMIN). ANULADA repone el stock")
    public BoletaResponse update(@PathVariable Long id, @Valid @RequestBody BoletaUpdateRequest request,
                                 @Parameter(hidden = true) UsuarioActual usuario) {
        return service.update(id, request, usuario);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una boleta ANULADA y sus detalles (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable Long id, @Parameter(hidden = true) UsuarioActual usuario) {
        service.delete(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
