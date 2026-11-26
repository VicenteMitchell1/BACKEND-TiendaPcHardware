package cl.pchardware.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos que el propio usuario puede cambiar en "Mi perfil". El rol, el estado y el RUT no se editan aquí. */
@Data
public class PerfilRequest {
    @Email(message = "El email no tiene un formato válido")
    @NotBlank(message = "El email es obligatorio")
    @Size(max = 100, message = "El email admite hasta 100 caracteres")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
    private String nombre;

    @Size(max = 100, message = "El apellido admite hasta 100 caracteres")
    private String apellido;
}
