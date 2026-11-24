package cl.pchardware.producto.service;

import cl.pchardware.producto.client.CategoriaClient;
import cl.pchardware.producto.dto.MovimientoStockRequest;
import cl.pchardware.producto.dto.ProductoRequest;
import cl.pchardware.producto.dto.ProductoResponse;
import cl.pchardware.producto.exception.BadRequestException;
import cl.pchardware.producto.exception.ConflictException;
import cl.pchardware.producto.exception.ResourceNotFoundException;
import cl.pchardware.producto.model.Producto;
import cl.pchardware.producto.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaClient categoriaClient;

    @Transactional(readOnly = true)
    public List<ProductoResponse> findAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> findActivos() {
        return repo.findByActivoTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse findById(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ProductoResponse create(ProductoRequest req) {
        String sku = req.getSku().trim();
        if (repo.existsBySku(sku)) {
            throw new ConflictException("Ya existe un producto con el SKU: " + sku);
        }
        Producto p = new Producto();
        aplicar(p, req, categoriaClient.obtenerNombre(req.getIdCategoria()));
        return toResponse(repo.save(p));
    }

    @Transactional
    public ProductoResponse update(Long id, ProductoRequest req) {
        Producto p = buscar(id);
        String sku = req.getSku().trim();
        if (repo.existsBySkuAndIdNot(sku, id)) {
            throw new ConflictException("Ya existe otro producto con el SKU: " + sku);
        }
        aplicar(p, req, categoriaClient.obtenerNombre(req.getIdCategoria()));
        return toResponse(repo.save(p));
    }

    /** Ajuste manual de stock de un producto (positivo repone, negativo descuenta), siempre atómico. */
    @Transactional
    public void updateStock(Long id, int cantidad) {
        Producto p = buscar(id);
        if (cantidad >= 0) {
            repo.reponerStock(id, cantidad);
        } else if (repo.descontarStock(id, -cantidad) == 0) {
            throw new BadRequestException("Stock insuficiente para: " + p.getNombre() + ". Disponible: " + p.getStock());
        }
    }

    /**
     * Descuenta el stock de todos los items de una compra en UNA transacción.
     * Si un producto no existe, está inactivo o no tiene stock, se lanza una excepción y se revierten
     * todos los descuentos anteriores: o se descuenta todo, o nada.
     * Devuelve los productos (con su precio vigente) en el mismo orden que los items.
     */
    @Transactional
    public List<ProductoResponse> descontarStock(MovimientoStockRequest req) {
        List<ProductoResponse> resultado = new ArrayList<>();
        for (MovimientoStockRequest.Item item : req.getItems()) {
            Producto p = buscar(item.getIdProducto());
            if (!Boolean.TRUE.equals(p.getActivo())) {
                throw new BadRequestException("El producto '" + p.getNombre() + "' no está disponible para la venta.");
            }
            if (repo.descontarStock(p.getId(), item.getCantidad()) == 0) {
                int disponible = repo.findStockById(p.getId()).orElse(0);
                throw new BadRequestException("Stock insuficiente para: " + p.getNombre()
                        + ". Disponibles: " + disponible + ", solicitados: " + item.getCantidad());
            }
            resultado.add(toResponse(buscar(p.getId())));
        }
        return resultado;
    }

    /** Devuelve al inventario las unidades de una compra anulada o fallida. */
    @Transactional
    public void reponerStock(MovimientoStockRequest req) {
        for (MovimientoStockRequest.Item item : req.getItems()) {
            if (repo.reponerStock(item.getIdProducto(), item.getCantidad()) == 0) {
                throw new ResourceNotFoundException("Producto no encontrado con ID: " + item.getIdProducto());
            }
        }
    }

    @Transactional(readOnly = true)
    public long contarPorCategoria(Long idCategoria) {
        return repo.countByIdCategoria(idCategoria);
    }

    @Transactional
    public int sincronizarNombreCategoria(Long idCategoria, String nombre) {
        return repo.actualizarNombreCategoria(idCategoria, nombre.trim());
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        repo.deleteById(id);
    }

    private Producto buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
    }

    private void aplicar(Producto p, ProductoRequest req, String nombreCategoria) {
        p.setSku(req.getSku().trim());
        p.setNombre(req.getNombre().trim());
        p.setMarca(req.getMarca());
        p.setModelo(req.getModelo());
        p.setDescripcion(req.getDescripcion());
        p.setPrecio(req.getPrecio());
        p.setPrecioAnterior(req.getPrecioAnterior());
        p.setStock(req.getStock());
        p.setImagen(req.getImagen());
        p.setEsOferta(Boolean.TRUE.equals(req.getEsOferta()));
        p.setPorcentajeDescuento(req.getPorcentajeDescuento());
        p.setActivo(req.getActivo() == null || req.getActivo());
        p.setIdCategoria(req.getIdCategoria());
        p.setNombreCategoria(nombreCategoria);
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
