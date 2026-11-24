package cl.pchardware.gateway.proxy;

import cl.pchardware.gateway.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Reenvía una petición del frontend al microservicio correspondiente.
 * Nunca copia cabeceras del cliente: las cabeceras de identidad (X-User-*) se construyen aquí
 * a partir del JWT ya validado, así el cliente no puede suplantar a otro usuario.
 * Los errores 4xx/5xx del microservicio se devuelven tal cual (ver GlobalExceptionHandler).
 */
@Component
@RequiredArgsConstructor
public class ProxyService {

    private final RestTemplate restTemplate;

    public ResponseEntity<String> get(String url) {
        return forward(HttpMethod.GET, url, null);
    }

    public ResponseEntity<String> forward(HttpMethod method, String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (body != null) headers.setContentType(MediaType.APPLICATION_JSON);
        agregarIdentidad(headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(resp.getStatusCode());
        return resp.hasBody() ? builder.contentType(MediaType.APPLICATION_JSON).body(resp.getBody()) : builder.build();
    }

    private static void agregarIdentidad(HttpHeaders headers) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            headers.set("X-User-Id", String.valueOf(u.id()));
            headers.set("X-User-Email", u.email());
            headers.set("X-User-Role", u.rol());
        }
    }
}
