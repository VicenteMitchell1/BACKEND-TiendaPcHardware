package cl.pchardware.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UsuarioRequest {
    @Email @NotBlank private String email;
    private String password;
    @NotBlank private String nombre;
    private String apellido;
    private String rut;
    private String rol;
    private Boolean activo;
}
