package cl.pchardware.gateway.client;

import cl.pchardware.gateway.dto.RegisterRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Acceso a usuario-service. La contraseña se verifica allá: el gateway nunca recibe el hash.
 * Errores de red se propagan (el manejador global responde 503).
 */
@Component
@RequiredArgsConstructor
public class UsuarioClient {

    private final RestTemplate restTemplate;

    @Value("${services.usuario-url:http://localhost:8081}")
    private String usuarioUrl;

    /** 401 si las credenciales no coinciden; 403 si la cuenta está desactivada. */
    public UsuarioSesion verificarCredenciales(String email, String password) {
        try {
            return restTemplate.postForObject(usuarioUrl + "/interno/auth/verificar",
                    Map.of("email", email, "password", password), UsuarioSesion.class);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new BadCredentialsException("Credenciales incorrectas.");
        } catch (HttpClientErrorException.Forbidden e) {
            throw new DisabledException("La cuenta está desactivada. Contacte al administrador.");
        }
    }

    public UsuarioSesion buscarPorId(Long id) {
        try {
            return restTemplate.getForObject(usuarioUrl + "/interno/usuarios/" + id, UsuarioSesion.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadCredentialsException("El usuario de la sesión ya no existe.");
        }
    }

    /** Los errores 400/409 (datos inválidos, email duplicado) llegan tal cual al frontend. */
    public UsuarioSesion registrar(RegisterRequest request) {
        return restTemplate.postForObject(usuarioUrl + "/interno/usuarios/register", request, UsuarioSesion.class);
    }

    @Data
    public static class UsuarioSesion {
        private Long id;
        private String email;
        private String nombre;
        private String apellido;
        private String rut;
        private String rol;
        private Boolean activo;
    }
}
