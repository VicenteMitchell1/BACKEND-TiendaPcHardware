package cl.pchardware.boleta.repository;

import cl.pchardware.boleta.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {
    List<Boleta> findAllByOrderByFechaEmisionDesc();
    Optional<Boleta> findByNumeroBoleta(String numeroBoleta);
    List<Boleta> findByIdUsuarioOrderByFechaEmisionDesc(Long idUsuario);
    boolean existsByNumeroBoleta(String numeroBoleta);
}
