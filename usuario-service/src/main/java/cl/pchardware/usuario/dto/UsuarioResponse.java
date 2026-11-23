package cl.pchardware.usuario.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UsuarioResponse {
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String rut;
    private String rol;
    private Boolean activo;
}
