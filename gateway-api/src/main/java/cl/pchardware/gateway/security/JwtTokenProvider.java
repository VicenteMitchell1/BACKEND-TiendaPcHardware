package cl.pchardware.gateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generarToken(String email, String rol, String nombre, Long id) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + jwtProperties.getExpirationMs());
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .claim("nombre", nombre)
                .claim("userId", id)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getKey())
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token invalido: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) { return getClaims(token).getSubject(); }
    public String getRolFromToken(String token) { return getClaims(token).get("rol", String.class); }
    public String getNombreFromToken(String token) { return getClaims(token).get("nombre", String.class); }

    public Long getUserIdFromToken(String token) {
        Object val = getClaims(token).get("userId");
        if (val instanceof Number num) return num.longValue();
        return null;
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }
}
