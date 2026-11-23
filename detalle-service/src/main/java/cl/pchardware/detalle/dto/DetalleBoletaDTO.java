package cl.pchardware.detalle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleBoletaDTO {
    private Long id;
    private Long idBoleta;

    @NotNull(message = "El ID del producto es obligatorio")
    private Long idProducto;

    private String nombreProducto;
    private String skuProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    private Integer precioUnitario;
    private Integer subtotal;
}
