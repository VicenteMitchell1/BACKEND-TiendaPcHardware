package cl.pchardware.categoria.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /** Cliente HTTP hacia otros microservicios: añade siempre la clave interna. */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, @Value("${internal.api-key:}") String internalApiKey) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().set(InternalApiKeyFilter.HEADER, internalApiKey);
                    return execution.execute(request, body);
                })
                .build();
    }
}
