package cl.pchardware.detalle.repository;

import cl.pchardware.detalle.model.DetalleBoleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DetalleBoletaRepository extends JpaRepository<DetalleBoleta, Long> {
    List<DetalleBoleta> findByIdBoleta(Long idBoleta);
    List<DetalleBoleta> findByIdBoletaIn(Collection<Long> idsBoleta);

    @Modifying
    @Query("DELETE FROM DetalleBoleta d WHERE d.idBoleta = :idBoleta")
    int deleteByIdBoleta(@Param("idBoleta") Long idBoleta);
}
