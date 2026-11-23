package cl.pchardware.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret = "dG9rZW5TZWNyZXRLZXlTb2Z0d2FyZVRpZW5kYTIwMjZQY0hhcmR3YXJlU2VjdXJpdHk=";
    private long expirationMs = 86400000;
}
