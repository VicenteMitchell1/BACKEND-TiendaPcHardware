package cl.pchardware.boleta.client;

import cl.pchardware.boleta.dto.DetalleBoletaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetalleClient {

    private final RestTemplate restTemplate;

    @Value("${services.detalle-url:http://localhost:8085}")
    private String detalleUrl;

    public List<DetalleBoletaDTO> findByBoletaId(Long idBoleta) {
        try {
            ResponseEntity<List<DetalleBoletaDTO>> response = restTemplate.exchange(
                    detalleUrl + "/detalles/boleta/" + idBoleta,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<DetalleBoletaDTO>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error obteniendo detalles para boleta {}: {}", idBoleta, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<DetalleBoletaDTO> saveBatch(List<DetalleBoletaDTO> dtos) {
        try {
            ResponseEntity<List<DetalleBoletaDTO>> response = restTemplate.exchange(
                    detalleUrl + "/detalles/batch",
                    HttpMethod.POST,
                    new HttpEntity<>(dtos),
                    new ParameterizedTypeReference<List<DetalleBoletaDTO>>() {}
            );
            return response.getBody() != null ? response.getBody() : dtos;
        } catch (Exception e) {
            log.error("Error guardando lote de detalles: {}", e.getMessage());
            return dtos;
        }
    }
}
