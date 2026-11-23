package cl.pchardware.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaRequest {
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;
    private String descripcion;
    private String icono;
    private Boolean activo;
}
