package cl.pchardware.detalle.controller;

import cl.pchardware.detalle.dto.DetalleBoletaDTO;
import cl.pchardware.detalle.service.DetalleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/detalles")
@Tag(name = "Detalles Boleta", description = "CRUD de items y lineas de boletas")
@RequiredArgsConstructor
public class DetalleController {

    private final DetalleService service;

    @GetMapping
    @Operation(summary = "Listar todos los detalles")
    public List<DetalleBoletaDTO> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle por ID")
    public DetalleBoletaDTO findById(@PathVariable Long id) { return service.findById(id); }

    @GetMapping("/boleta/{idBoleta}")
    @Operation(summary = "Obtener todos los detalles de una boleta específica")
    public List<DetalleBoletaDTO> findByBoletaId(@PathVariable Long idBoleta) {
        return service.findByBoletaId(idBoleta);
    }

    @PostMapping
    @Operation(summary = "Crear un detalle de boleta individual")
    public ResponseEntity<DetalleBoletaDTO> create(@Valid @RequestBody DetalleBoletaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/batch")
    @Operation(summary = "Crear un lote de detalles de boleta")
    public ResponseEntity<List<DetalleBoletaDTO>> createBatch(@Valid @RequestBody List<DetalleBoletaDTO> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBatch(dtos));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar detalle por ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
