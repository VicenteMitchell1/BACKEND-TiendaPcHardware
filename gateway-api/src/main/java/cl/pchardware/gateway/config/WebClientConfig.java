package cl.pchardware.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /** Cabecera con la clave compartida que exigen todos los microservicios. */
    public static final String INTERNAL_HEADER = "X-Internal-Token";

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, @Value("${internal.api-key:}") String internalApiKey) {
        if (!StringUtils.hasText(internalApiKey)) {
            throw new IllegalStateException("Falta la clave interna: defina la variable de entorno INTERNAL_API_KEY.");
        }
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(15))
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().set(INTERNAL_HEADER, internalApiKey);
                    return execution.execute(request, body);
                })
                .build();
    }
}
