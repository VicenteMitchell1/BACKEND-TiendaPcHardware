package cl.pchardware.producto.client;

import cl.pchardware.producto.exception.BadRequestException;
import cl.pchardware.producto.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Valida contra categoria-service que la categoría de un producto exista (integridad PRODUCTO -> CATEGORIA). */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoriaClient {

    private final RestTemplate restTemplate;

    @Value("${services.categoria-url:http://localhost:8082}")
    private String categoriaUrl;

    /** Devuelve el nombre de la categoría; 400 si no existe y 503 si categoria-service no responde. */
    public String obtenerNombre(Long idCategoria) {
        try {
            Map<?, ?> resp = restTemplate.getForObject(categoriaUrl + "/categorias/" + idCategoria, Map.class);
            if (resp == null || !(resp.get("nombre") instanceof String nombre)) {
                throw new ServiceUnavailableException("Respuesta inválida de categoria-service.");
            }
            return nombre;
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("La categoría con ID " + idCategoria + " no existe.");
        } catch (RestClientException e) {
            log.error("No se pudo consultar categoria-service para {}: {}", idCategoria, e.getMessage());
            throw new ServiceUnavailableException("No se pudo validar la categoría. Intente más tarde.");
        }
    }
}
