package cl.pchardware.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cambio de contraseña por el propio usuario: exige la contraseña actual. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioPasswordRequest {
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @NotBlank(message = "La contraseña nueva es obligatoria")
    @Size(min = 4, max = 72, message = "La contraseña debe tener entre 4 y 72 caracteres")
    private String passwordNueva;
}
