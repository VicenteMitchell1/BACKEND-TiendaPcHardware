package cl.pchardware.boleta.client;

import cl.pchardware.boleta.exception.BadRequestException;
import cl.pchardware.boleta.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductoClient {

    private final RestTemplate restTemplate;

    @Value("${services.producto-url:http://localhost:8083}")
    private String productoUrl;

    public ProductoDTO findById(Long id) {
        try {
            return restTemplate.getForObject(productoUrl + "/productos/" + id, ProductoDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Producto no encontrado con ID: " + id);
        } catch (Exception e) {
            log.error("Error consultando producto-service para id {}: {}", id, e.getMessage());
            throw new BadRequestException("No se pudo verificar el producto en el catálogo.");
        }
    }

    public void updateStock(Long idProducto, int cantidad) {
        try {
            restTemplate.postForEntity(
                    productoUrl + "/productos/" + idProducto + "/stock",
                    new HttpEntity<>(Map.of("cantidad", cantidad)),
                    Void.class
            );
        } catch (Exception e) {
            log.error("Error actualizando stock para producto {}: {}", idProducto, e.getMessage());
            throw new BadRequestException("No se pudo actualizar el stock del producto " + idProducto);
        }
    }

    @Data
    public static class ProductoDTO {
        private Long id;
        private String sku;
        private String nombre;
        private Integer precio;
        private Integer stock;
    }
}
