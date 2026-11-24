package cl.pchardware.producto.repository;

import cl.pchardware.producto.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    List<Producto> findByActivoTrue();
    long countByIdCategoria(Long idCategoria);

    /**
     * Descuento atómico: la base de datos comprueba el stock y lo resta en una sola sentencia,
     * bloqueando la fila. Dos compras simultáneas nunca pueden vender la misma unidad.
     * Devuelve 1 si se descontó, 0 si no había stock suficiente o el producto está inactivo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock - :cantidad " +
           "WHERE p.id = :id AND p.activo = true AND p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.stock = p.stock + :cantidad WHERE p.id = :id")
    int reponerStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    @Query("SELECT p.stock FROM Producto p WHERE p.id = :id")
    Optional<Integer> findStockById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Producto p SET p.nombreCategoria = :nombre WHERE p.idCategoria = :idCategoria")
    int actualizarNombreCategoria(@Param("idCategoria") Long idCategoria, @Param("nombre") String nombre);
}
