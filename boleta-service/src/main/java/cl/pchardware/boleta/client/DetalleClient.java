package cl.pchardware.boleta.client;

import cl.pchardware.boleta.dto.DetalleBoletaDTO;
import cl.pchardware.boleta.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cliente de detalle-service. Los errores se propagan como 503 en lugar de ocultarse:
 * nunca se responde como si los detalles se hubieran guardado o leído cuando no fue así.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DetalleClient {

    private static final ParameterizedTypeReference<List<DetalleBoletaDTO>> LISTA = new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;

    @Value("${services.detalle-url:http://localhost:8085}")
    private String detalleUrl;

    public List<DetalleBoletaDTO> findByBoletaId(Long idBoleta) {
        try {
            List<DetalleBoletaDTO> r = restTemplate.exchange(detalleUrl + "/detalles/boleta/" + idBoleta,
                    HttpMethod.GET, null, LISTA).getBody();
            return r != null ? r : List.of();
        } catch (RestClientException e) {
            throw noDisponible("leer los detalles de la boleta " + idBoleta, e);
        }
    }

    /** Detalles de varias boletas en UNA llamada, agrupados por id de boleta. */
    public Map<Long, List<DetalleBoletaDTO>> findByBoletaIds(Collection<Long> idsBoleta) {
        if (idsBoleta.isEmpty()) return Map.of();
        String ids = idsBoleta.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            List<DetalleBoletaDTO> r = restTemplate.exchange(detalleUrl + "/detalles/boletas?ids=" + ids,
                    HttpMethod.GET, null, LISTA).getBody();
            return (r != null ? r : List.<DetalleBoletaDTO>of()).stream()
                    .collect(Collectors.groupingBy(DetalleBoletaDTO::getIdBoleta));
        } catch (RestClientException e) {
            throw noDisponible("leer los detalles de las boletas", e);
        }
    }

    public List<DetalleBoletaDTO> saveBatch(List<DetalleBoletaDTO> dtos) {
        try {
            List<DetalleBoletaDTO> r = restTemplate.exchange(detalleUrl + "/detalles/batch",
                    HttpMethod.POST, new HttpEntity<>(dtos), LISTA).getBody();
            if (r == null || r.size() != dtos.size()) throw new ServiceUnavailableException("Respuesta inválida de detalle-service.");
            return r;
        } catch (RestClientException e) {
            throw noDisponible("guardar los detalles de la compra", e);
        }
    }

    public void deleteByBoletaId(Long idBoleta) {
        try {
            restTemplate.delete(detalleUrl + "/detalles/boleta/" + idBoleta);
        } catch (RestClientException e) {
            throw noDisponible("eliminar los detalles de la boleta " + idBoleta, e);
        }
    }

    private ServiceUnavailableException noDisponible(String accion, RestClientException e) {
        log.error("detalle-service no pudo {}: {}", accion, e.getMessage());
        return new ServiceUnavailableException("No se pudo " + accion + ": servicio de detalles no disponible. Intente más tarde.");
    }
}
