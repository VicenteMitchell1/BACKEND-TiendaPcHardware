package cl.pchardware.detalle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Línea de detalle de una boleta. El subtotal siempre lo calcula el servidor (precioUnitario x cantidad). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleBoletaDTO {
    private Long id;

    @NotNull(message = "El ID de la boleta es obligatorio")
    private Long idBoleta;

    @NotNull(message = "El ID del producto es obligatorio")
    private Long idProducto;

    private String nombreProducto;
    private String skuProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    @NotNull(message = "El precio unitario es obligatorio")
    @Min(value = 0, message = "El precio unitario no puede ser negativo")
    private Integer precioUnitario;

    private Integer subtotal;
}
