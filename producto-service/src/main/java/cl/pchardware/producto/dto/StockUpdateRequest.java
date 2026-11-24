package cl.pchardware.producto.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ajuste manual de stock de un producto: cantidad positiva repone, negativa descuenta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequest {
    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;
}
