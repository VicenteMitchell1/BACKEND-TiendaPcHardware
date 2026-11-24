package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

/** Esquemas Swagger de productos (el gateway reenvía el JSON tal cual a producto-service). */
public final class ProductoDocs {
    private ProductoDocs() {}

    @Schema(name = "ProductoRequest", description = "Datos para crear o actualizar un producto")
    public record Request(
            @Schema(example = "GPU-RTX-4070S", requiredMode = Schema.RequiredMode.REQUIRED) String sku,
            @Schema(example = "NVIDIA RTX 4070 Super", requiredMode = Schema.RequiredMode.REQUIRED) String nombre,
            @Schema(example = "NVIDIA") String marca,
            @Schema(example = "4070 Super") String modelo,
            @Schema(example = "12GB GDDR6X, DLSS 3") String descripcion,
            @Schema(example = "689990", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED) Integer precio,
            @Schema(example = "749990", minimum = "0") Integer precioAnterior,
            @Schema(example = "15", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED) Integer stock,
            @Schema(example = "/static/img/productos/rtx-4070s.jpg") String imagen,
            @Schema(example = "true") Boolean esOferta,
            @Schema(example = "8", minimum = "0", maximum = "100") Integer porcentajeDescuento,
            @Schema(example = "true") Boolean activo,
            @Schema(example = "1", description = "Debe existir en categoria-service", requiredMode = Schema.RequiredMode.REQUIRED) Long idCategoria) {}

    @Schema(name = "Producto")
    public record Response(
            @Schema(example = "1") Long id,
            @Schema(example = "GPU-RTX-4090FE") String sku,
            @Schema(example = "NVIDIA RTX 4090 Founders Edition") String nombre,
            @Schema(example = "NVIDIA") String marca,
            @Schema(example = "Founders Edition") String modelo,
            @Schema(example = "24GB GDDR6X, Ada Lovelace") String descripcion,
            @Schema(example = "1599990") Integer precio,
            @Schema(example = "1799990") Integer precioAnterior,
            @Schema(example = "10") Integer stock,
            @Schema(example = "/static/img/productos/rtx-4090.jpg") String imagen,
            @Schema(example = "true") Boolean esOferta,
            @Schema(example = "11") Integer porcentajeDescuento,
            @Schema(example = "true") Boolean activo,
            @Schema(example = "1") Long idCategoria,
            @Schema(example = "Tarjetas Gráficas") String nombreCategoria) {}
}
