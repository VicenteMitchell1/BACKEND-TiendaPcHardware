package cl.pchardware.boleta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CuponRequest {
    @NotBlank(message = "El código del cupón es obligatorio")
    @Size(min = 3, max = 30, message = "El código debe tener entre 3 y 30 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "El código sólo admite letras, números, guion y guion bajo")
    @Schema(example = "HARDWARE2026")
    private String codigo;

    @NotNull(message = "El porcentaje es obligatorio")
    @Min(value = 1, message = "El porcentaje mínimo es 1")
    @Max(value = 90, message = "El porcentaje máximo es 90")
    @Schema(example = "15")
    private Integer porcentaje;

    @Size(max = 200, message = "La descripción admite hasta 200 caracteres")
    private String descripcion;

    private Boolean activo;
}
