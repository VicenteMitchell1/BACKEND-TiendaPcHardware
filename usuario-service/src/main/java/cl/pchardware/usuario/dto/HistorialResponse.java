package cl.pchardware.usuario.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class HistorialResponse {
    private Long id;
    private LocalDateTime fecha;
    private Long idUsuario;
    private String emailUsuario;
    private Long idAutor;
    private String emailAutor;
    private String rolAutor;
    private String accion;
    private String detalle;
}
