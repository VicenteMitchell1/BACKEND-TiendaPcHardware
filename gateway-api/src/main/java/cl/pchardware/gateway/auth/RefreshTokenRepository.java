package cl.pchardware.gateway.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.idUsuario = :idUsuario AND r.revocado = false")
    int revocarTodosDelUsuario(@Param("idUsuario") Long idUsuario);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiraEn < :limite")
    int eliminarExpirados(@Param("limite") Instant limite);
}
