package cl.pchardware.detalle.service;

import cl.pchardware.detalle.dto.DetalleBoletaDTO;
import cl.pchardware.detalle.exception.ResourceNotFoundException;
import cl.pchardware.detalle.model.DetalleBoleta;
import cl.pchardware.detalle.repository.DetalleBoletaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetalleService {

    private final DetalleBoletaRepository repo;

    @Transactional(readOnly = true)
    public List<DetalleBoletaDTO> findAll() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DetalleBoletaDTO> findByBoletaId(Long idBoleta) {
        return repo.findByIdBoleta(idBoleta).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DetalleBoletaDTO findById(Long id) {
        return toDTO(repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado: " + id)));
    }

    @Transactional
    public DetalleBoletaDTO create(DetalleBoletaDTO dto) {
        int subtotal = (dto.getPrecioUnitario() != null ? dto.getPrecioUnitario() : 0) * (dto.getCantidad() != null ? dto.getCantidad() : 1);
        DetalleBoleta d = DetalleBoleta.builder()
                .idBoleta(dto.getIdBoleta())
                .idProducto(dto.getIdProducto())
                .nombreProducto(dto.getNombreProducto() != null ? dto.getNombreProducto() : "Producto " + dto.getIdProducto())
                .skuProducto(dto.getSkuProducto())
                .cantidad(dto.getCantidad())
                .precioUnitario(dto.getPrecioUnitario())
                .subtotal(dto.getSubtotal() != null ? dto.getSubtotal() : subtotal)
                .build();
        return toDTO(repo.save(d));
    }

    @Transactional
    public List<DetalleBoletaDTO> createBatch(List<DetalleBoletaDTO> dtos) {
        List<DetalleBoleta> entities = dtos.stream().map(dto -> {
            int subtotal = (dto.getPrecioUnitario() != null ? dto.getPrecioUnitario() : 0) * (dto.getCantidad() != null ? dto.getCantidad() : 1);
            return DetalleBoleta.builder()
                    .idBoleta(dto.getIdBoleta())
                    .idProducto(dto.getIdProducto())
                    .nombreProducto(dto.getNombreProducto() != null ? dto.getNombreProducto() : "Producto " + dto.getIdProducto())
                    .skuProducto(dto.getSkuProducto())
                    .cantidad(dto.getCantidad())
                    .precioUnitario(dto.getPrecioUnitario())
                    .subtotal(dto.getSubtotal() != null ? dto.getSubtotal() : subtotal)
                    .build();
        }).collect(Collectors.toList());
        return repo.saveAll(entities).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Detalle no encontrado: " + id);
        repo.deleteById(id);
    }

    private DetalleBoletaDTO toDTO(DetalleBoleta d) {
        return DetalleBoletaDTO.builder()
                .id(d.getId())
                .idBoleta(d.getIdBoleta())
                .idProducto(d.getIdProducto())
                .nombreProducto(d.getNombreProducto())
                .skuProducto(d.getSkuProducto())
                .cantidad(d.getCantidad())
                .precioUnitario(d.getPrecioUnitario())
                .subtotal(d.getSubtotal())
                .build();
    }
}
