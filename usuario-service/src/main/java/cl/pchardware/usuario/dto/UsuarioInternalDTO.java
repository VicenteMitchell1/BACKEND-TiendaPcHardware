package cl.pchardware.usuario.dto;

import lombok.Data;

@Data
public class UsuarioInternalDTO {
    private Long id;
    private String email;
    private String passwordHash;
    private String nombre;
    private String apellido;
    private String rut;
    private String rol;
    private Boolean activo;
}
