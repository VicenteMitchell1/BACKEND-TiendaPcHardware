package cl.pchardware.categoria.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CategoriaResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private String icono;
    private Boolean activo;
}
