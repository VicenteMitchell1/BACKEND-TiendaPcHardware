package cl.pchardware.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @Email @NotBlank private String email;
    @NotBlank private String password;
    @NotBlank private String nombre;
    private String apellido;
    private String rut;
}
