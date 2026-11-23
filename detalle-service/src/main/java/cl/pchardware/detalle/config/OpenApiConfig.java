package cl.pchardware.detalle.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Detalle Service API", version = "1.0", description = "Microservicio de Detalle Boleta"))
public class OpenApiConfig {}
