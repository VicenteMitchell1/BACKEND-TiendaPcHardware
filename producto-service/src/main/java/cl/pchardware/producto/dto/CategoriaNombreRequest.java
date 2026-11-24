package cl.pchardware.producto.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaNombreRequest {
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    private String nombre;
}
