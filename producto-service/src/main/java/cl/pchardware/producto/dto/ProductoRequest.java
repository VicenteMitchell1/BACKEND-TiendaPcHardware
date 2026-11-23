package cl.pchardware.producto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductoRequest {
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombre;

    private String marca;
    private String modelo;
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Integer precio;

    private Integer precioAnterior;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    private String imagen;
    private Boolean esOferta;
    private Integer porcentajeDescuento;
    private Boolean activo;

    @NotNull(message = "El ID de categoría es obligatorio")
    private Long idCategoria;
}
