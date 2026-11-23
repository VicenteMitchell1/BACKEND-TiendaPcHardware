package cl.pchardware.producto.service;

import cl.pchardware.producto.dto.ProductoRequest;
import cl.pchardware.producto.dto.ProductoResponse;
import cl.pchardware.producto.exception.BadRequestException;
import cl.pchardware.producto.exception.ResourceNotFoundException;
import cl.pchardware.producto.model.Producto;
import cl.pchardware.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repo;
    private final RestTemplate restTemplate;

    @Value("${services.categoria-url:http://localhost:8082}")
    private String categoriaUrl;

    @Transactional(readOnly = true)
    public List<ProductoResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> findActivos() {
        return repo.findByActivoTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoResponse findById(Long id) {
        return toResponse(repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id)));
    }

    @Transactional
    public ProductoResponse create(ProductoRequest req) {
        if (repo.existsBySku(req.getSku())) {
            throw new BadRequestException("Ya existe un producto con el SKU: " + req.getSku());
        }
        String nombreCat = fetchNombreCategoria(req.getIdCategoria());
        Producto p = buildFromRequest(req, nombreCat);
        return toResponse(repo.save(p));
    }

    @Transactional
    public ProductoResponse update(Long id, ProductoRequest req) {
        Producto p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        String nombreCat = fetchNombreCategoria(req.getIdCategoria());

        p.setSku(req.getSku());
        p.setNombre(req.getNombre());
        p.setMarca(req.getMarca());
        p.setModelo(req.getModelo());
        p.setDescripcion(req.getDescripcion());
        p.setPrecio(req.getPrecio());
        p.setPrecioAnterior(req.getPrecioAnterior());
        p.setStock(req.getStock());
        p.setImagen(req.getImagen());
        p.setEsOferta(req.getEsOferta() != null ? req.getEsOferta() : false);
        p.setPorcentajeDescuento(req.getPorcentajeDescuento());
        p.setActivo(req.getActivo() != null ? req.getActivo() : true);
        p.setIdCategoria(req.getIdCategoria());
        p.setNombreCategoria(nombreCat);

        return toResponse(repo.save(p));
    }

    @Transactional
    public void updateStock(Long id, int cantidad) {
        Producto p = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));

        int nuevoStock = p.getStock() + cantidad;
        if (nuevoStock < 0) {
            throw new BadRequestException("Stock insuficiente para: " + p.getNombre() + ". Disponible: " + p.getStock());
        }
        p.setStock(nuevoStock);
        repo.save(p);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        repo.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    private String fetchNombreCategoria(Long idCategoria) {
        try {
            Map<String, Object> resp = restTemplate.getForObject(categoriaUrl + "/categorias/" + idCategoria, Map.class);
            if (resp != null && resp.containsKey("nombre")) {
                return (String) resp.get("nombre");
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener información de la categoría {}: {}", idCategoria, e.getMessage());
        }
        return "Categoría " + idCategoria;
    }

    private Producto buildFromRequest(ProductoRequest req, String nombreCat) {
        return Producto.builder()
                .sku(req.getSku())
                .nombre(req.getNombre())
                .marca(req.getMarca())
                .modelo(req.getModelo())
                .descripcion(req.getDescripcion())
                .precio(req.getPrecio())
                .precioAnterior(req.getPrecioAnterior())
                .stock(req.getStock())
                .imagen(req.getImagen())
                .esOferta(req.getEsOferta() != null ? req.getEsOferta() : false)
                .porcentajeDescuento(req.getPorcentajeDescuento())
                .activo(req.getActivo() != null ? req.getActivo() : true)
                .idCategoria(req.getIdCategoria())
                .nombreCategoria(nombreCat)
                .build();
    }

    private ProductoResponse toResponse(Producto p) {
        return ProductoResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .nombre(p.getNombre())
                .marca(p.getMarca())
                .modelo(p.getModelo())
                .descripcion(p.getDescripcion())
                .precio(p.getPrecio())
                .precioAnterior(p.getPrecioAnterior())
                .stock(p.getStock())
                .imagen(p.getImagen())
                .esOferta(p.getEsOferta())
                .porcentajeDescuento(p.getPorcentajeDescuento())
                .activo(p.getActivo())
                .idCategoria(p.getIdCategoria())
                .nombreCategoria(p.getNombreCategoria())
                .build();
    }
}
