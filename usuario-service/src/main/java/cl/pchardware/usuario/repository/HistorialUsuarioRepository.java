package cl.pchardware.usuario.repository;

import cl.pchardware.usuario.model.HistorialUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialUsuarioRepository extends JpaRepository<HistorialUsuario, Long> {
    List<HistorialUsuario> findTop200ByOrderByFechaDescIdDesc();
    List<HistorialUsuario> findByIdUsuarioOrderByFechaDescIdDesc(Long idUsuario);
}
