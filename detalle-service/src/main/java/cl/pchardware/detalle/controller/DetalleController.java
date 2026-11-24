package cl.pchardware.detalle.controller;

import cl.pchardware.detalle.dto.DetalleBoletaDTO;
import cl.pchardware.detalle.service.DetalleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/boletas")
    @Operation(summary = "Obtener los detalles de varias boletas en una sola consulta (?ids=1,2,3)")
    public List<DetalleBoletaDTO> findByBoletaIds(@RequestParam List<Long> ids) {
        return service.findByBoletaIds(ids);
    }

    @PostMapping
    @Operation(summary = "Crear un detalle de boleta individual")
    public ResponseEntity<DetalleBoletaDTO> create(@Valid @RequestBody DetalleBoletaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/batch")
    @Operation(summary = "Crear un lote de detalles de boleta (todo o nada)")
    public ResponseEntity<List<DetalleBoletaDTO>> createBatch(
            @RequestBody @NotEmpty(message = "El lote de detalles no puede estar vacío") List<@Valid DetalleBoletaDTO> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBatch(dtos));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un detalle (el subtotal se recalcula)")
    public DetalleBoletaDTO update(@PathVariable Long id, @Valid @RequestBody DetalleBoletaDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar detalle por ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/boleta/{idBoleta}")
    @Operation(summary = "Eliminar todos los detalles de una boleta (usado al eliminar la boleta)")
    public Map<String, Integer> deleteByBoleta(@PathVariable Long idBoleta) {
        return Map.of("eliminados", service.deleteByBoletaId(idBoleta));
    }
}
