package cl.pchardware.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredencialesRequest {
    @NotBlank(message = "El email es obligatorio") private String email;
    @NotBlank(message = "La contraseña es obligatoria") private String password;
}
