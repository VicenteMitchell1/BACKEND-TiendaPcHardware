package cl.pchardware.gateway.dto.doc;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Esquemas Swagger de boletas y sus detalles. */
public final class BoletaDocs {
    private BoletaDocs() {}

    @Schema(name = "ItemCompra")
    public record Item(
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long idProducto,
            @Schema(example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) Integer cantidad) {}

    @Schema(name = "BoletaRequest", description = "Compra. Montos, email e idUsuario NO se envían: los calcula el servidor desde el catálogo y el JWT")
    public record Request(
            @Schema(example = "HARDWARE2026", description = "Cupón opcional; el servidor valida el código y calcula el descuento") String codigoCupon,
            @Schema(example = "Webpay Plus / Débito") String metodoPago,
            @Schema(example = "Panchito Gamer") String nombreCliente,
            @Schema(example = "18555444-3") String rutCliente,
            @Schema(example = "Av. Providencia 1234, Depto 402") String direccionEnvio,
            @Schema(example = "Providencia") String comuna,
            @Schema(example = "Región Metropolitana de Santiago") String region,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Item> items) {}

    @Schema(name = "BoletaUpdateRequest", description = "Edición administrativa. Pasar a ANULADA repone el stock y es irreversible")
    public record UpdateRequest(
            @Schema(example = "ENVIADA", allowableValues = {"PAGADA", "ENVIADA", "ENTREGADA", "ANULADA"}) String estado,
            @Schema(example = "Transferencia") String metodoPago,
            @Schema(example = "Panchito Gamer") String nombreCliente,
            @Schema(example = "18555444-3") String rutCliente,
            @Schema(example = "Av. Providencia 1234, Depto 402") String direccionEnvio,
            @Schema(example = "Providencia") String comuna,
            @Schema(example = "Región Metropolitana de Santiago") String region) {}

    @Schema(name = "DetalleBoleta")
    public record Detalle(
            @Schema(example = "1") Long id,
            @Schema(example = "1") Long idBoleta,
            @Schema(example = "3") Long idProducto,
            @Schema(example = "Memoria RAM Corsair Vengeance 32GB DDR5") String nombreProducto,
            @Schema(example = "RAM-CORSAIR-32G") String skuProducto,
            @Schema(example = "1") Integer cantidad,
            @Schema(example = "180000") Integer precioUnitario,
            @Schema(example = "180000", description = "Calculado: precioUnitario x cantidad") Integer subtotal) {}

    @Schema(name = "DetalleBoletaRequest", description = "Corrección administrativa de una línea (el subtotal se recalcula)")
    public record DetalleRequest(
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long idBoleta,
            @Schema(example = "3", requiredMode = Schema.RequiredMode.REQUIRED) Long idProducto,
            @Schema(example = "Memoria RAM Corsair Vengeance 32GB DDR5") String nombreProducto,
            @Schema(example = "RAM-CORSAIR-32G") String skuProducto,
            @Schema(example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) Integer cantidad,
            @Schema(example = "180000", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED) Integer precioUnitario) {}

    @Schema(name = "Boleta")
    public record Response(
            @Schema(example = "1") Long id,
            @Schema(example = "BOL-18392044") String numeroBoleta,
            @Schema(example = "2026-11-23T18:42:10") String fechaEmision,
            @Schema(example = "180000") Integer subtotal,
            @Schema(example = "0") Integer descuento,
            @Schema(example = "HARDWARE2026", nullable = true) String codigoCupon,
            @Schema(example = "180000") Integer total,
            @Schema(example = "Webpay Plus / Débito") String metodoPago,
            @Schema(example = "PAGADA") String estado,
            @Schema(example = "Panchito Gamer") String nombreCliente,
            @Schema(example = "pancho@gmail.com") String emailCliente,
            @Schema(example = "18555444-3") String rutCliente,
            @Schema(example = "Av. Providencia 1234, Depto 402") String direccionEnvio,
            @Schema(example = "Providencia") String comuna,
            @Schema(example = "Región Metropolitana de Santiago") String region,
            @Schema(example = "4") Long idUsuario,
            List<Detalle> detalles) {}
}
