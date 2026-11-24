package cl.pchardware.boleta.client;

import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ResourceNotFoundException;
import cl.pchardware.boleta.exception.ServiceUnavailableException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoClient {

    private final RestTemplate restTemplate;

    @Value("${services.producto-url:http://localhost:8083}")
    private String productoUrl;

    /** Producto y cantidad de una línea de compra. */
    public record ItemStock(Long idProducto, Integer cantidad) {}

    /**
     * Descuenta en producto-service el stock de TODOS los items en una sola transacción (todo o nada)
     * y devuelve los productos con su precio vigente, en el mismo orden.
     */
    public List<ProductoDTO> descontarStock(List<ItemStock> items) {
        try {
            List<ProductoDTO> productos = restTemplate.exchange(
                    productoUrl + "/productos/stock/descontar", HttpMethod.POST,
                    new HttpEntity<>(Map.of("items", items)),
                    new ParameterizedTypeReference<List<ProductoDTO>>() {}).getBody();
            if (productos == null || productos.size() != items.size()) {
                throw new ServiceUnavailableException("Respuesta inválida de producto-service.");
            }
            return productos;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(ErroresRemotos.mensaje(e, "Producto no encontrado."));
        } catch (HttpClientErrorException e) {
            throw new BadRequestException(ErroresRemotos.mensaje(e, "No se pudo reservar el stock."));
        } catch (RestClientException e) {
            log.error("Error descontando stock en producto-service: {}", e.getMessage());
            throw new ServiceUnavailableException("El catálogo no está disponible. Intente más tarde.");
        }
    }

    /** Devuelve las unidades al inventario (anulación o compensación de una compra fallida). */
    public void reponerStock(List<ItemStock> items) {
        try {
            restTemplate.postForEntity(productoUrl + "/productos/stock/reponer", Map.of("items", items), Void.class);
        } catch (RestClientException e) {
            log.error("Error reponiendo stock en producto-service: {}", e.getMessage());
            throw new ServiceUnavailableException("No se pudo reponer el stock. Intente más tarde.");
        }
    }

    @Data
    public static class ProductoDTO {
        private Long id;
        private String sku;
        private String nombre;
        private Integer precio;
        private Integer stock;
        private Boolean activo;
    }
}
