package cl.pchardware.gateway.auth;

import cl.pchardware.gateway.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Renovación segura de sesiones:
 * - El refresh token es un valor aleatorio de 256 bits (no un JWT) y se guarda sólo su hash.
 * - Rotación: cada uso lo invalida y entrega uno nuevo.
 * - Detección de reutilización: si alguien presenta un token ya usado (posible robo),
 *   se revocan todas las sesiones de ese usuario.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repo;
    private final JwtProperties props;

    /** Crea un refresh token para el usuario y devuelve el valor en claro (sólo se entrega una vez). */
    @Transactional
    public String crear(Long idUsuario) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant ahora = Instant.now();
        repo.save(RefreshToken.builder()
                .tokenHash(hash(token))
                .idUsuario(idUsuario)
                .creadoEn(ahora)
                .expiraEn(ahora.plusMillis(props.getRefreshExpirationMs()))
                .revocado(false)
                .build());
        return token;
    }

    /**
     * Valida y consume un refresh token. Devuelve el id del usuario dueño.
     * Lanza BadCredentialsException (401) si no existe, expiró o ya fue usado/revocado.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public Long consumir(String token) {
        RefreshToken rt = buscar(token);
        if (rt.isRevocado()) {
            int revocados = repo.revocarTodosDelUsuario(rt.getIdUsuario());
            log.warn("Reutilización de refresh token del usuario {}: se revocaron {} sesiones", rt.getIdUsuario(), revocados);
            throw new BadCredentialsException("Sesión inválida. Inicie sesión nuevamente.");
        }
        if (rt.getExpiraEn().isBefore(Instant.now())) {
            throw new BadCredentialsException("La sesión expiró. Inicie sesión nuevamente.");
        }
        rt.setRevocado(true);
        return rt.getIdUsuario();
    }

    /** Cierra la sesión asociada al refresh token. Es idempotente. */
    @Transactional
    public void revocar(String token) {
        if (!StringUtils.hasText(token)) return;
        repo.findByTokenHash(hash(token)).ifPresent(rt -> rt.setRevocado(true));
    }

    /** Limpieza de tokens vencidos (se invoca al emitir nuevos para no requerir tareas programadas). */
    @Transactional
    public void limpiarExpirados() {
        repo.eliminarExpirados(Instant.now());
    }

    private RefreshToken buscar(String token) {
        if (!StringUtils.hasText(token)) throw new BadCredentialsException("Refresh token requerido.");
        return repo.findByTokenHash(hash(token))
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida. Inicie sesión nuevamente."));
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
