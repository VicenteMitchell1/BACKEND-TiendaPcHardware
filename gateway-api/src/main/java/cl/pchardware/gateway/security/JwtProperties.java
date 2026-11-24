package cl.pchardware.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de tokens. El secreto NO tiene valor por defecto en el código:
 * se entrega con la variable de entorno JWT_SECRET (ver .env.example).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** Clave HMAC en Base64 (mínimo 256 bits = 32 bytes). */
    private String secret;
    /** Vida del access token (por defecto 1 hora). */
    private long expirationMs = 3_600_000;
    /** Vida del refresh token (por defecto 7 días). */
    private long refreshExpirationMs = 604_800_000;
}
