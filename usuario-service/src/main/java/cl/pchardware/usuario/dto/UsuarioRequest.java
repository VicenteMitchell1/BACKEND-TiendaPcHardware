package cl.pchardware.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Alta y edición de usuarios por el administrador. La contraseña es obligatoria sólo al crear. */
@Data
public class UsuarioRequest {
    @Email(message = "El email no tiene un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    @Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String apellido;
    private String rut;
    /** ADMIN, VENDEDOR o CLIENTE (por defecto CLIENTE). */
    private String rol;
    private Boolean activo;
}
