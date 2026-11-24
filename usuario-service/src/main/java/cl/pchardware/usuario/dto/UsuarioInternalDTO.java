package cl.pchardware.usuario.dto;

import lombok.Data;

/** Datos de sesión que consume el API Gateway. No incluye el hash de la contraseña. */
@Data
public class UsuarioInternalDTO {
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private String rut;
    private String rol;
    private Boolean activo;
}
