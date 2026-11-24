package cl.pchardware.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

/**
 * Genera y valida los access tokens (JWT firmados con HMAC-SHA).
 * La clave se construye una sola vez al arrancar: si falta o es débil (< 256 bits) la aplicación no inicia.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(JwtProperties props) {
        if (!StringUtils.hasText(props.getSecret())) {
            throw new IllegalStateException("Falta el secreto JWT: defina la variable de entorno JWT_SECRET (Base64, mínimo 32 bytes).");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
        this.expirationMs = props.getExpirationMs();
    }

    public String generarToken(UsuarioAutenticado usuario) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(usuario.email())
                .claim("rol", usuario.rol())
                .claim("nombre", usuario.nombre())
                .claim("userId", usuario.id())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Valida firma y expiración. Devuelve vacío si el token es inválido, está alterado o expiró. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object id = c.get("userId");
            String rol = c.get("rol", String.class);
            if (!(id instanceof Number n) || !StringUtils.hasText(rol)) return Optional.empty();
            return Optional.of(new UsuarioAutenticado(n.longValue(), c.getSubject(), rol, c.get("nombre", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token rechazado: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
