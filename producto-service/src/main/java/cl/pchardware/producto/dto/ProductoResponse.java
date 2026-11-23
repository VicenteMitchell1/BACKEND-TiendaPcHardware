package cl.pchardware.producto.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ProductoResponse {
    private Long id;
    private String sku;
    private String nombre;
    private String marca;
    private String modelo;
    private String descripcion;
    private Integer precio;
    private Integer precioAnterior;
    private Integer stock;
    private String imagen;
    private Boolean esOferta;
    private Integer porcentajeDescuento;
    private Boolean activo;
    private Long idCategoria;
    private String nombreCategoria;
}
