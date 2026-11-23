package cl.pchardware.boleta.controller;

import cl.pchardware.boleta.dto.BoletaRequest;
import cl.pchardware.boleta.dto.BoletaResponse;
import cl.pchardware.boleta.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/boletas")
@Tag(name = "Boletas", description = "Emisión y consulta de boletas y órdenes de compra")
@RequiredArgsConstructor
public class BoletaController {

    private final BoletaService service;

    @GetMapping
    @Operation(summary = "Listar todas las boletas emitidas (ADMIN y VENDEDOR)")
    public List<BoletaResponse> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener boleta por ID")
    public BoletaResponse findById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/numero/{numero}")
    @Operation(summary = "Buscar boleta por número de boleta")
    public BoletaResponse findByNumero(@PathVariable String numero) { return service.findByNumero(numero); }

    @GetMapping("/mis-boletas")
    @Operation(summary = "Listar boletas del usuario autenticado")
    public List<BoletaResponse> findMisBoletas(
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return service.findMisBoletas(email, userId);
    }

    @PostMapping
    @Operation(summary = "Emitir nueva boleta de compra con descuento automático de stock")
    public ResponseEntity<BoletaResponse> create(
            @Valid @RequestBody BoletaRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, email, userId));
    }
}
