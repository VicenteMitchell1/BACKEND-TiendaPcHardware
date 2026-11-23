package cl.pchardware.producto.repository;

import cl.pchardware.producto.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsBySku(String sku);
    List<Producto> findByActivoTrue();
    List<Producto> findByIdCategoria(Long idCategoria);
}
