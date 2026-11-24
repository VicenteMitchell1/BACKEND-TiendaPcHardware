package cl.pchardware.producto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Lote de productos y cantidades que se descuentan (compra) o reponen (anulación) en una sola transacción. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockRequest {

    @NotEmpty(message = "Debe indicar al menos un producto")
    @Valid
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull(message = "El ID del producto es obligatorio")
        private Long idProducto;

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer cantidad;
    }
}
