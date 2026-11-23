package cl.pchardware.gateway.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String rut;
    private String rol;
}
