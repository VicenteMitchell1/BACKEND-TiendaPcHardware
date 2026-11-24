package cl.pchardware.gateway.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Refresh token emitido al iniciar sesión. Sólo se guarda el hash SHA-256 (nunca el token en claro),
 * así una filtración de la base de datos no permite suplantar sesiones.
 */
@Entity
@Table(name = "REFRESH_TOKEN", indexes = @Index(name = "idx_refresh_usuario", columnList = "id_usuario"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocado", nullable = false)
    private boolean revocado;
}
