package cl.pchardware.gateway.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioClient {

    @Value("${services.usuario-url:http://localhost:8081}")
    private String usuarioUrl;

    private final RestTemplate restTemplate;

    public UsuarioInternalDTO findByEmail(String email) {
        try {
            return restTemplate.getForObject(
                    usuarioUrl + "/interno/usuarios/email/" + email,
                    UsuarioInternalDTO.class
            );
        } catch (RestClientException e) {
            log.error("Error llamando usuario-service para email {}: {}", email, e.getMessage());
            return null;
        }
    }

    @Data
    public static class UsuarioInternalDTO {
        private Long id;
        private String email;
        private String passwordHash;
        private String nombre;
        private String apellido;
        private String rut;
        private String rol;
        private Boolean activo;
    }
}
