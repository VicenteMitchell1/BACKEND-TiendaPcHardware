package cl.pchardware.detalle.service;

import cl.pchardware.detalle.dto.DetalleBoletaDTO;
import cl.pchardware.detalle.exception.ResourceNotFoundException;
import cl.pchardware.detalle.model.DetalleBoleta;
import cl.pchardware.detalle.repository.DetalleBoletaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DetalleService {

    private final DetalleBoletaRepository repo;

    @Transactional(readOnly = true)
    public List<DetalleBoletaDTO> findAll() {
        return repo.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DetalleBoletaDTO> findByBoletaId(Long idBoleta) {
        return repo.findByIdBoleta(idBoleta).stream().map(this::toDTO).toList();
    }

    /** Detalles de varias boletas en una sola consulta (evita una llamada HTTP por boleta). */
    @Transactional(readOnly = true)
    public List<DetalleBoletaDTO> findByBoletaIds(Collection<Long> idsBoleta) {
        if (idsBoleta == null || idsBoleta.isEmpty()) return List.of();
        return repo.findByIdBoletaIn(idsBoleta).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DetalleBoletaDTO findById(Long id) {
        return toDTO(buscar(id));
    }

    @Transactional
    public DetalleBoletaDTO create(DetalleBoletaDTO dto) {
        DetalleBoleta d = new DetalleBoleta();
        aplicar(d, dto);
        return toDTO(repo.save(d));
    }

    /** Guarda todas las líneas de una boleta en una transacción: si una falla, no se guarda ninguna. */
    @Transactional
    public List<DetalleBoletaDTO> createBatch(List<DetalleBoletaDTO> dtos) {
        List<DetalleBoleta> entidades = dtos.stream().map(dto -> {
            DetalleBoleta d = new DetalleBoleta();
            aplicar(d, dto);
            return d;
        }).toList();
        return repo.saveAll(entidades).stream().map(this::toDTO).toList();
    }

    @Transactional
    public DetalleBoletaDTO update(Long id, DetalleBoletaDTO dto) {
        DetalleBoleta d = buscar(id);
        aplicar(d, dto);
        return toDTO(repo.save(d));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Detalle no encontrado: " + id);
        repo.deleteById(id);
    }

    @Transactional
    public int deleteByBoletaId(Long idBoleta) {
        return repo.deleteByIdBoleta(idBoleta);
    }

    private DetalleBoleta buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado: " + id));
    }

    private void aplicar(DetalleBoleta d, DetalleBoletaDTO dto) {
        d.setIdBoleta(dto.getIdBoleta());
        d.setIdProducto(dto.getIdProducto());
        d.setNombreProducto(dto.getNombreProducto() != null ? dto.getNombreProducto() : "Producto " + dto.getIdProducto());
        d.setSkuProducto(dto.getSkuProducto());
        d.setCantidad(dto.getCantidad());
        d.setPrecioUnitario(dto.getPrecioUnitario());
        d.setSubtotal(dto.getPrecioUnitario() * dto.getCantidad());
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
