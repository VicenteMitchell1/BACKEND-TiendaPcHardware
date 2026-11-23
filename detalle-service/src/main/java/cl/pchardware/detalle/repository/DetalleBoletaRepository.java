package cl.pchardware.detalle.repository;

import cl.pchardware.detalle.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {
    List<DetalleBoleta> findByIdBoleta(Long idBoleta);
    List<DetalleBoleta> findByIdProducto(Long idProducto);
}
