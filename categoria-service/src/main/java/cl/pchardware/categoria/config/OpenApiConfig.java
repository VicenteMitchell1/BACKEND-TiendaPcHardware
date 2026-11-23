package cl.pchardware.categoria.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Categoria Service API", version = "1.0", description = "Microservicio de Categorias"))
public class OpenApiConfig {}
