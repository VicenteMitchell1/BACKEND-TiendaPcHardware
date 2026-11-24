package cl.pchardware.categoria.client;

import cl.pchardware.categoria.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/** Consultas a producto-service para mantener la integridad entre CATEGORIA y PRODUCTO. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoClient {

    private final RestTemplate restTemplate;

    @Value("${services.producto-url:http://localhost:8083}")
    private String productoUrl;

    /** Cantidad de productos asociados a la categoría. */
    public long contarProductos(Long idCategoria) {
        try {
            Map<?, ?> resp = restTemplate.getForObject(productoUrl + "/productos/categoria/" + idCategoria + "/total", Map.class);
            Object total = resp != null ? resp.get("total") : null;
            return total instanceof Number n ? n.longValue() : 0L;
        } catch (RestClientException e) {
            log.error("No se pudo consultar producto-service: {}", e.getMessage());
            throw new ServiceUnavailableException("No se pudo verificar si la categoría tiene productos. Intente más tarde.");
        }
    }

    /** Propaga el nuevo nombre de la categoría a los productos (que lo guardan para mostrarlo en la tienda). */
    public void sincronizarNombre(Long idCategoria, String nombre) {
        try {
            restTemplate.exchange(productoUrl + "/productos/categoria/" + idCategoria + "/nombre",
                    HttpMethod.PUT, new HttpEntity<>(Map.of("nombre", nombre)), Void.class);
        } catch (RestClientException e) {
            log.error("No se pudo sincronizar el nombre de la categoría {}: {}", idCategoria, e.getMessage());
            throw new ServiceUnavailableException("No se pudo actualizar el nombre en los productos. Intente más tarde.");
        }
    }
}
