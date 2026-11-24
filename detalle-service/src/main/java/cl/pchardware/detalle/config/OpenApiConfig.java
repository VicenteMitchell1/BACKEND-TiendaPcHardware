package cl.pchardware.detalle.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Detalle Service API", version = "1.0",
                description = "Microservicio de líneas de detalle de boletas. Servicio interno: requiere la cabecera X-Internal-Token (el frontend debe usar el API Gateway)."),
        security = @SecurityRequirement(name = "internalToken"))
@SecurityScheme(name = "internalToken", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-Internal-Token")
public class OpenApiConfig {}
