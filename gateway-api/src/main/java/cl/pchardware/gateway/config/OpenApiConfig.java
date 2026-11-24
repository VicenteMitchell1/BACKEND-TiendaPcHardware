package cl.pchardware.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "PC Hardware API Gateway", version = "1.0",
                description = "Punto único de entrada de la tienda PC Hardware. Autenticación JWT (Bearer) con roles "
                        + "ADMIN, VENDEDOR y CLIENTE. Para probar endpoints protegidos: POST /api/v1/auth/login, copiar el "
                        + "campo 'token' y pulsar 'Authorize'."),
        servers = @Server(url = "http://localhost:8080", description = "Entorno local / Docker"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
